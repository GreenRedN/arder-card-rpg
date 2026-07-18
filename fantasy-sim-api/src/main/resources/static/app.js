(() => {
  "use strict";

  const STORAGE_KEY = "arder.story.resume.v1";
  const $ = (selector) => document.querySelector(selector);
  const $$ = (selector) => Array.from(document.querySelectorAll(selector));
  const state = { race: "human", credentials: null, campaign: null, busy: false };

  const elements = {
    landing: $("#landing"), game: $("#game"), form: $("#start-form"), name: $("#player-name"),
    resume: $("#resume-button"), resumeLabel: $("#resume-label"), loading: $("#loading"), toast: $("#toast"),
    chat: $("#chat-log"), choices: $("#choice-list"), statusPanel: $("#status-panel"),
    journalPanel: $("#journal-panel"), backdrop: $("#drawer-backdrop")
  };

  function loadCredentials() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
      if (saved?.campaignId && saved?.accessToken) {
        state.credentials = saved;
        elements.resume.classList.remove("hidden");
        elements.resumeLabel.textContent = saved.playerName || "저장된 이야기";
      }
    } catch (_) {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  function saveCredentials(campaignId, accessToken, playerName) {
    state.credentials = { campaignId, accessToken, playerName };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.credentials));
  }

  async function api(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const response = await fetch(path, { ...options, headers });
    let payload = null;
    try { payload = await response.json(); } catch (_) { /* handled below */ }
    if (!response.ok) throw new Error(payload?.message || "서버와 통신할 수 없습니다.");
    return payload;
  }

  function setBusy(busy) {
    state.busy = busy;
    elements.loading.classList.toggle("hidden", !busy);
    $$(".choice-button, #start-button, #resume-button").forEach((button) => button.disabled = busy);
  }

  function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.remove("hidden");
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => elements.toast.classList.add("hidden"), 3200);
  }

  async function startCampaign(event) {
    event.preventDefault();
    const playerName = elements.name.value.trim();
    if (!playerName) return elements.name.focus();
    setBusy(true);
    try {
      const envelope = await api("/api/v2/campaigns", {
        method: "POST",
        body: JSON.stringify({ playerName, race: state.race })
      });
      saveCredentials(envelope.campaign.campaignId, envelope.accessToken, envelope.campaign.player.name);
      showGame(envelope.campaign);
    } catch (error) {
      showToast(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function resumeCampaign() {
    if (!state.credentials) return;
    setBusy(true);
    try {
      const { campaignId, accessToken } = state.credentials;
      const envelope = await api(`/api/v2/campaigns/${campaignId}`, {
        headers: { "X-Campaign-Token": accessToken }
      });
      showGame(envelope.campaign);
    } catch (error) {
      showToast(error.message);
      if (error.message.includes("토큰") || error.message.includes("찾을 수")) {
        localStorage.removeItem(STORAGE_KEY);
        state.credentials = null;
        elements.resume.classList.add("hidden");
      }
    } finally {
      setBusy(false);
    }
  }

  async function choose(choice) {
    if (state.busy || choice.locked || !state.credentials) return;
    setBusy(true);
    const requestId = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
    try {
      const envelope = await api(`/api/v2/campaigns/${state.credentials.campaignId}/choices`, {
        method: "POST",
        headers: { "X-Campaign-Token": state.credentials.accessToken },
        body: JSON.stringify({ choiceId: choice.id, requestId })
      });
      render(envelope.campaign);
    } catch (error) {
      showToast(error.message);
    } finally {
      setBusy(false);
    }
  }

  function showGame(campaign) {
    elements.landing.classList.add("hidden");
    elements.game.classList.remove("hidden");
    render(campaign);
  }

  function render(campaign) {
    state.campaign = campaign;
    const relation = campaign.relationships?.[0] || { trust: 0, affinity: 0, guard: 0 };

    text("#chapter-label", campaign.world.chapterTitle);
    text("#location-label", campaign.world.locationName);
    text("#time-label", campaign.timeLabel);
    text("#weather-label", campaign.world.weather);
    text("#scene-title", campaign.scene?.title || "이야기 종료");
    text("#objective-label", campaign.world.objective);
    text("#objective-card-text", campaign.world.objective);
    text("#objective-location", campaign.world.locationName);
    text("#turn-label", `${campaign.turn}번째 선택`);
    text("#recap-text", campaign.recap);

    text("#companion-name", relation.name || "세라 아벨린");
    text("#companion-role", relation.role || "태양교단 조사관");
    text("#relation-stage", relation.stageLabel || "낯선 사이");
    text("#companion-mood", campaign.scene?.mood || "고요");
    setMeter("trust", relation.trust || 0);
    setMeter("affinity", relation.affinity || 0);
    setMeter("guard", relation.guard || 0);

    text("#player-card-name", campaign.player.name);
    text("#player-meta", `${campaign.player.raceLabel} · ${campaign.player.jobLabel}`);
    text("#hp-value", campaign.player.hp);
    text("#power-value", campaign.player.power);
    text("#insight-value", campaign.player.insight);
    text("#gold-value", campaign.player.gold);
    text("#alignment-label", `성향 · ${campaign.player.alignment}`);
    text("#empire-value", campaign.world.empireStability);
    text("#demon-value", campaign.world.demonInfluence);
    text("#mood-value", campaign.world.publicMood);

    renderCharacterStage(campaign, relation);
    renderMessages(campaign.messages || []);
    renderDirector(campaign.director);
    renderChoices(campaign.scene?.choices || [], campaign.director);
    renderMemories(campaign.memories || []);
    renderInventory(campaign.player.inventory || []);
  }

  function renderCharacterStage(campaign, relation) {
    const mood = campaign.scene?.mood || "고요";
    const companionName = relation.name || "세라 아벨린";
    const companionRole = relation.role || "태양교단 조사관";
    const latestNpc = [...(campaign.messages || [])].reverse()
      .find((message) => message.role?.toUpperCase() === "NPC");
    const memoryCount = campaign.memories?.length || 0;
    const stage = $("#character-stage");

    text("#stage-companion-name", companionName);
    text("#stage-companion-role", companionRole);
    text("#stage-dialogue", latestNpc?.text || "당신의 다음 대답을 조용히 기다리고 있다.");
    text("#stage-mood", mood);
    text("#stage-relation", relation.stageLabel || "낯선 사이");
    text("#stage-memory-hint", memoryCount
      ? `함께 기억하는 사건 ${memoryCount}개`
      : "아직 공유된 기억이 없다");
    text("#relationship-caption", relationshipCaption(relation));

    if (stage) {
      stage.dataset.tone = moodTone(mood);
      stage.dataset.place = placeTone(campaign.world?.locationId);
    }
  }

  function relationshipCaption(relation) {
    const trust = relation.trust || 0;
    const affinity = relation.affinity || 0;
    const guard = relation.guard || 0;
    if (guard >= 65) return "세라는 당신의 의도를 의심하며 말과 행동을 세심하게 경계한다.";
    if (trust >= 70 && affinity >= 55) return "세라는 당신을 믿을 수 있는 특별한 동료로 받아들이고 있다.";
    if (trust >= 70) return "세라는 위험한 순간에도 당신의 판단을 믿고 등을 맡긴다.";
    if (affinity >= 55) return "세라는 임무 밖의 감정까지 조금씩 당신에게 드러내고 있다.";
    if (trust >= 35) return "세라는 당신을 경계의 대상이 아닌 동료로 보기 시작했다.";
    return "세라는 아직 당신의 말보다 다음 행동을 조심스럽게 살피고 있다.";
  }

  function moodTone(mood) {
    if (/전투|분노|절박|위험|긴장/.test(mood)) return "danger";
    if (/미소|안도|신뢰|다정|평온/.test(mood)) return "warm";
    if (/집중|추적|의심|불안|경계/.test(mood)) return "focus";
    return "neutral";
  }

  function placeTone(locationId = "") {
    if (locationId.includes("copse")) return "forest";
    if (locationId.includes("chapel")) return "chapel";
    if (locationId.includes("lantern")) return "inn";
    if (locationId.includes("aqueduct")) return "aqueduct";
    if (locationId.includes("dune")) return "desert";
    return "gate";
  }

  function renderMessages(messages) {
    const fragment = document.createDocumentFragment();
    const latestNpcIndex = messages.findLastIndex
      ? messages.findLastIndex((message) => message.role?.toUpperCase() === "NPC")
      : messages.map((message) => message.role?.toUpperCase()).lastIndexOf("NPC");
    messages.forEach((message, index) => {
      const article = document.createElement("article");
      article.className = `message ${message.role.toLowerCase()}`;
      if (index === latestNpcIndex) article.classList.add("is-latest");
      article.dataset.id = message.id;
      const meta = document.createElement("div");
      meta.className = "message-meta";
      const speaker = document.createElement("b");
      speaker.textContent = message.speakerName;
      const mood = document.createElement("span");
      mood.textContent = message.mood || "";
      meta.append(speaker, mood);
      const body = document.createElement("div");
      body.className = "message-body";
      body.textContent = message.text;
      article.append(meta, body);
      fragment.append(article);
    });
    elements.chat.replaceChildren(fragment);
    requestAnimationFrame(scrollChatToLatest);
  }

  function scrollChatToLatest() {
    elements.chat.scrollTop = elements.chat.scrollHeight;
  }

  function renderDirector(director = {}) {
    const live = director.source === "LIVE_AI" && director.liveAi;
    const status = $("#director-status");
    text("#director-status", live ? "LIVE AI" : "규칙 대체");
    if (status) status.dataset.mode = live ? "LIVE_AI" : "RULE_FALLBACK";
    text("#director-intent", director.intent || "현재 장면의 목표와 관계 상태를 분석합니다.");
    text("#director-memory-count", director.recalledMemoryIds?.length || 0);
    text("#director-constraint-count", director.constraints?.length || 0);
    text("#director-reason", director.spotlightReason || "열린 카드만 주목하며 게임 규칙은 변경하지 않습니다.");
    text("#director-model", live && director.model ? `모델 · ${director.model}` : "AI API 미연결 · 규칙 기반 대체 연출");
  }

  function renderChoices(choices, director = {}) {
    const fragment = document.createDocumentFragment();
    choices.forEach((choice, index) => {
      const directorPick = !choice.locked && choice.id === director.spotlightChoiceId;
      const button = document.createElement("button");
      button.type = "button";
      button.className = "choice-button";
      button.classList.toggle("is-director-pick", directorPick);
      button.disabled = choice.locked || state.busy;
      button.title = choice.locked
        ? (choice.lockedReason || "잠긴 선택지")
        : `${choice.text}${directorPick ? " · 내러티브 디렉터가 주목한 카드" : ""}`;
      button.dataset.category = choiceCategory(choice.category);
      button.dataset.risk = choice.risk || "";
      const leading = document.createElement("span");
      leading.className = "choice-leading";
      leading.append(createIcon(choice.locked ? "lock" : choiceIcon(choice.category), "choice-symbol"));
      const number = document.createElement("span");
      number.className = "choice-index";
      number.textContent = String(index + 1).padStart(2, "0");
      leading.append(number);
      const copy = document.createElement("span");
      copy.className = "choice-copy";
      const label = document.createElement("b");
      label.textContent = choice.text;
      const category = document.createElement("small");
      category.textContent = choice.locked ? choice.lockedReason : choice.category;
      const meta = document.createElement("span");
      meta.className = "choice-meta";
      meta.append(category);
      if (directorPick) {
        const badge = document.createElement("span");
        badge.className = "director-badge";
        badge.textContent = director.source === "LIVE_AI" ? "AI 주목" : "디렉터 주목";
        meta.append(badge);
      }
      copy.append(label, meta);
      const risk = document.createElement("span");
      risk.className = "risk";
      risk.textContent = choice.risk;
      button.append(leading, copy, risk);
      button.addEventListener("click", () => choose(choice));
      fragment.append(button);
    });
    elements.choices.replaceChildren(fragment);
  }

  function createIcon(name, className) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    const use = document.createElementNS("http://www.w3.org/2000/svg", "use");
    svg.classList.add(className);
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    use.setAttribute("href", `/assets/icons.svg#${name}`);
    svg.append(use);
    return svg;
  }

  function choiceIcon(category = "") {
    if (/관계|신뢰|대화|맹세|동료/.test(category)) return "chat";
    if (/거래|계약|협상/.test(category)) return "scales";
    if (/조사|추적|전술|의뢰|기록/.test(category)) return "search";
    if (/전투|구조|결단|압박/.test(category)) return "sword";
    if (/은닉|기만|비밀/.test(category)) return "eye";
    if (/휴식|회복/.test(category)) return "moon";
    if (/종족|특기|마력/.test(category)) return "spark";
    if (/방어|보호|교단/.test(category)) return "shield";
    return "compass";
  }

  function choiceCategory(category = "") {
    if (/전투|위험|압박/.test(category)) return "danger";
    if (/관계|신뢰|대화|맹세/.test(category)) return "relation";
    if (/은닉|기만|비밀/.test(category)) return "secret";
    if (/종족|특기|마력/.test(category)) return "special";
    return "standard";
  }

  function renderMemories(memories) {
    text("#memory-count", memories.length);
    if (!memories.length) {
      const empty = document.createElement("div");
      empty.className = "memory-empty";
      empty.textContent = "아직 기록된 중요한 기억이 없습니다.";
      $("#memory-list").replaceChildren(empty);
      return;
    }
    const nodes = memories.slice(0, 8).map((memory) => {
      const item = document.createElement("article");
      item.className = "memory-item";
      const title = document.createElement("b");
      title.textContent = memory.title;
      const summary = document.createElement("p");
      summary.textContent = memory.summary;
      item.append(title, summary);
      return item;
    });
    $("#memory-list").replaceChildren(...nodes);
  }

  function renderInventory(items) {
    const list = $("#inventory-list");
    if (!items.length) {
      const empty = document.createElement("span");
      empty.className = "memory-empty";
      empty.textContent = "소지품 없음";
      list.replaceChildren(empty);
      return;
    }
    list.replaceChildren(...items.map((name) => {
      const item = document.createElement("span");
      item.className = "inventory-item";
      item.textContent = name;
      return item;
    }));
  }

  function setMeter(id, value) {
    const safe = Math.max(0, Math.min(100, value));
    $(`#${id}-bar`).style.width = `${safe}%`;
    text(`#${id}-value`, safe);
  }

  function text(selector, value) {
    const node = $(selector);
    if (node) node.textContent = value ?? "";
  }

  let drawerTrigger = null;

  function isMobileLayout() {
    return window.matchMedia("(max-width: 940px)").matches;
  }

  function closeDrawers(restoreFocus = false) {
    const previousTrigger = drawerTrigger;
    elements.statusPanel.classList.remove("open");
    elements.journalPanel.classList.remove("open");
    elements.backdrop.classList.add("hidden");
    $$('[data-drawer]').forEach((button) => button.setAttribute("aria-expanded", "false"));
    if (isMobileLayout()) {
      elements.statusPanel.setAttribute("aria-hidden", "true");
      elements.journalPanel.setAttribute("aria-hidden", "true");
    } else {
      elements.statusPanel.removeAttribute("aria-hidden");
      elements.journalPanel.removeAttribute("aria-hidden");
    }
    $(".story-stage").inert = false;
    drawerTrigger = null;
    if (restoreFocus && previousTrigger) previousTrigger.focus();
  }

  function syncDrawerAccessibility() {
    if (isMobileLayout()) {
      [elements.statusPanel, elements.journalPanel].forEach((panel) => {
        panel.setAttribute("aria-hidden", String(!panel.classList.contains("open")));
      });
      return;
    }
    closeDrawers(false);
  }

  $$(".race-card").forEach((button) => button.addEventListener("click", () => {
    state.race = button.dataset.race;
    $$(".race-card").forEach((item) => {
      const selected = item === button;
      item.classList.toggle("selected", selected);
      item.setAttribute("aria-pressed", String(selected));
    });
  }));

  $$('[data-drawer]').forEach((button) => button.addEventListener("click", () => {
    closeDrawers(false);
    const panel = button.dataset.drawer === "status" ? elements.statusPanel : elements.journalPanel;
    drawerTrigger = button;
    panel.classList.add("open");
    panel.setAttribute("aria-hidden", "false");
    button.setAttribute("aria-expanded", "true");
    $(".story-stage").inert = true;
    elements.backdrop.classList.remove("hidden");
    panel.focus({ preventScroll: true });
  }));
  elements.backdrop.addEventListener("click", () => closeDrawers(true));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && drawerTrigger) closeDrawers(true);
  });
  window.addEventListener("resize", () => {
    syncDrawerAccessibility();
    if (!elements.game.classList.contains("hidden")) requestAnimationFrame(scrollChatToLatest);
  });
  elements.form.addEventListener("submit", startCampaign);
  elements.resume.addEventListener("click", resumeCampaign);
  $("#new-story-button").addEventListener("click", () => {
    if (!confirm("새 캠페인을 시작하면 이 브라우저의 이어하기 연결이 새 기록으로 교체됩니다. 시작 화면으로 돌아갈까요?")) return;
    closeDrawers(false);
    elements.game.classList.add("hidden");
    elements.landing.classList.remove("hidden");
    elements.resume.classList.toggle("hidden", !state.credentials);
  });

  syncDrawerAccessibility();
  loadCredentials();
})();
