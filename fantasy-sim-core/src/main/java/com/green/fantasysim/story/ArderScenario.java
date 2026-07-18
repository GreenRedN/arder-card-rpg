package com.green.fantasysim.story;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Curated vertical-slice scenario for Arder. Cards are deterministic rule objects;
 * an optional narrative provider may rewrite only narration and dialogue.
 */
final class ArderScenario {
    static final String ID = "ash-oath";
    static final String INITIAL_SCENE = "arrival";
    static final String COMPANION_ID = "sera";

    private ArderScenario() {}

    static StoryScene scene(StoryCampaign c, String sceneId) {
        return switch (sceneId) {
            case "arrival" -> arrival(c);
            case "wagon" -> wagon(c);
            case "seal_reveal" -> sealReveal(c);
            case "forest_tracks" -> forestTracks(c);
            case "ambush" -> ambush(c);
            case "chapel" -> chapel(c);
            case "oath" -> oath(c);
            case "hub" -> hub(c);
            case "sera_talk" -> seraTalk(c);
            case "guild_job" -> guildJob(c);
            case "west_road" -> westRoad(c);
            case "secret_archive" -> secretArchive(c);
            case "rest" -> rest(c);
            default -> throw new IllegalArgumentException("unknown scene: " + sceneId);
        };
    }

    private static StoryScene arrival(StoryCampaign c) {
        StoryScene s = base("arrival", "비 내리는 성문", "solania-west-gate", "솔라니아 제국 서문",
                "수상한 교단 수레의 흔적을 확인한다",
                "세라", "태양교단 조사관", "경계",
                "해가 저문 솔라니아 제국의 서문. 비에 젖은 피난민 행렬 옆으로 태양교단의 봉인이 찍힌 수레가 쓰러져 있다. 검은 외투의 여인이 당신 앞을 막아선다.",
                "나는 세라 아벨린. 저 수레에는 사람들에게 보여선 안 될 물건이 있었어요. 당신, 방금 수레 쪽에서 왔죠? 잠깐만 도와줄래요?");

        s.choices.add(remember(choice("arrival_help", "망설이지 않고 돕겠다고 한다",
                "사람부터 살펴보죠. 설명은 움직이면서 들을게요.", "관계", "안전", "wagon", 10,
                e -> { e.dTrust = 8; e.dAffinity = 4; e.dGood = 2; }),
                "PROMISE", "첫 번째 동행", c.player.name + "은 세라의 조사를 돕기로 했다.", 82, "sera", "promise"));
        s.choices.add(remember(choice("arrival_price", "먼저 보수와 위험을 묻는다",
                "도울 수는 있습니다. 위험과 보수를 먼저 말해주시죠.", "거래", "보통", "wagon", 12,
                e -> { e.dGold = 8; e.dTrust = 2; e.dNeutral = 2; }),
                "CONTRACT", "조건부 협력", c.player.name + "은 보수를 조건으로 세라의 의뢰를 맡았다.", 60, "sera", "contract"));
        s.choices.add(remember(choice("arrival_watch", "대답 대신 수레와 여인을 관찰한다",
                "대답하기 전에 확인할 게 있습니다. 정말 교단의 수레가 맞습니까?",
                "조사", "안전", "wagon", 15,
                e -> { e.dInsight = 2; e.dGuard = 3; e.dNeutral = 1; }),
                "IMPRESSION", "신중한 첫인상", "세라는 " + c.player.name + "이 쉽게 믿지 않는 사람임을 기억한다.", 48, "sera", "impression"));
        return s;
    }

    private static StoryScene wagon(StoryCampaign c) {
        StoryScene s = base("wagon", "부서진 봉인", "solania-west-gate", "솔라니아 제국 서문",
                "수레에서 사라진 물건의 정체를 밝힌다",
                "세라", "태양교단 조사관", "집중",
                "수레의 문짝은 안쪽에서 바깥으로 뜯겨 있다. 바닥에는 은빛 가루와 검붉은 진흙이 엉겨 붙었고, 운전수는 흔적도 없이 사라졌다.",
                "교단 기록에는 평범한 성유라고 적혀 있어요. 하지만 평범한 물건이라면 누군가 이렇게까지 훔쳐가진 않았겠죠.");

        s.choices.add(choice("wagon_seal", "부서진 태양교단 봉인을 조사한다",
                "봉인이 깨진 방향부터 보죠. 누가 무엇을 숨겼는지 흔적이 남았을 겁니다.",
                "조사", "안전", "seal_reveal", 20,
                e -> { e.dInsight = 2; e.dTrust = 3; }));
        s.choices.add(choice("wagon_tracks", "검붉은 발자국을 따라간다",
                "진흙이 아직 젖어 있습니다. 멀리 가지 못했을 거예요.",
                "추적", "위험", "forest_tracks", 35,
                e -> { e.dPower = 1; e.dTrust = 2; }));

        switch (c.player.race) {
            case "elf" -> s.choices.add(remember(choice("wagon_race", "[엘프] 은빛 가루에 남은 왜곡된 마력을 읽는다",
                    "이건 성유가 아닙니다. 기억을 비트는 마력이 묻어 있어요.",
                    "종족 특기", "보통", "seal_reveal", 15,
                    e -> { e.dInsight = 4; e.dTrust = 6; }),
                    "DISCOVERY", "왜곡의 흔적", "엘프의 감각으로 수레에서 기억 왜곡 마력을 찾아냈다.", 88, "magic", "clue"));
            case "beast" -> s.choices.add(remember(choice("wagon_race", "[수인] 빗속에 남은 피와 향 냄새를 좇는다",
                    "피 냄새 사이에 낯선 향이 섞여 있습니다. 서쪽 숲으로 이어져요.",
                    "종족 특기", "보통", "forest_tracks", 20,
                    e -> { e.dInsight = 3; e.dAffinity = 4; }),
                    "DISCOVERY", "향 냄새의 추적", "수인의 후각으로 습격자가 서쪽 숲으로 향했음을 밝혀냈다.", 82, "track", "clue"));
            case "dwarf" -> s.choices.add(remember(choice("wagon_race", "[드워프] 수레의 이중 바닥을 찾아낸다",
                    "이 못은 운송용이 아닙니다. 아래에 숨겨진 칸이 하나 더 있어요.",
                    "종족 특기", "안전", "seal_reveal", 20,
                    e -> { e.dGold = 5; e.dInsight = 3; e.dTrust = 5; e.addItem = "부러진 은제 열쇠"; }),
                    "DISCOVERY", "수레의 이중 바닥", "드워프의 안목으로 숨겨진 적재함과 은제 열쇠를 발견했다.", 86, "craft", "clue"));
            default -> s.choices.add(remember(choice("wagon_race", "[인간] 운송장과 성문 통행 기록을 대조한다",
                    "기록의 필체가 다릅니다. 누군가 교단 서명을 흉내 냈군요.",
                    "종족 특기", "안전", "seal_reveal", 25,
                    e -> { e.dInsight = 3; e.dTrust = 4; e.dEmpire = 1; }),
                    "DISCOVERY", "위조된 운송장", "성문 기록을 대조해 교단 운송장이 위조됐음을 밝혀냈다.", 84, "document", "clue"));
        }
        return s;
    }

    private static StoryScene sealReveal(StoryCampaign c) {
        StoryScene s = base("seal_reveal", "달빛을 품은 파편", "solania-west-gate", "솔라니아 제국 서문",
                "월식 파편을 어떻게 처리할지 결정한다",
                "세라", "태양교단 조사관", "불안",
                "봉인의 안쪽에서 손톱만 한 검은 결정이 떨어진다. 비구름 너머 달빛이 닿자 결정 속에서 사람의 속삭임 같은 진동이 번진다.",
                "월식 마경에서 나온 파편이에요. 교단 안에서도 이 운송을 아는 사람은 셋뿐이었는데… 내부에 배신자가 있어요.");
        s.choices.add(remember(choice("seal_give", "파편을 세라에게 넘긴다",
                "당신이 보관하세요. 대신 이제부터는 숨기는 것 없이 말해줘야 합니다.",
                "신뢰", "안전", "ambush", 15,
                e -> { e.dTrust = 10; e.dAffinity = 4; e.dGood = 2; e.dDemon = -1; e.setFlag = "sera_has_shard"; e.setFlagValue = "true"; }),
                "TRUST", "월식 파편을 맡기다", c.player.name + "은 월식 파편을 세라에게 맡기고 진실을 요구했다.", 94, "sera", "shard", "trust"));
        s.choices.add(remember(choice("seal_keep", "파편을 몰래 챙겨둔다",
                "깨진 봉인뿐입니다. 파편 같은 건 보이지 않는군요.",
                "은닉", "매우 위험", "ambush", 15,
                e -> { e.dGuard = 10; e.dEvil = 2; e.dPower = 2; e.dDemon = 2; e.addItem = "월식 파편"; e.setFlag = "hid_shard"; e.setFlagValue = "true"; }),
                "SECRET", "숨겨둔 월식 파편", c.player.name + "은 세라에게 숨긴 채 월식 파편을 챙겼다.", 98, "sera", "shard", "secret"));
        s.choices.add(remember(choice("seal_destroy", "파편을 태양의 불꽃으로 파괴하도록 한다",
                "증거보다 사람이 먼저입니다. 지금 여기서 파괴하죠.",
                "결단", "보통", "ambush", 20,
                e -> { e.dTrust = 5; e.dGood = 4; e.dDemon = -4; e.dInsight = 1; e.setFlag = "destroyed_shard"; e.setFlagValue = "true"; }),
                "DECISION", "파편의 정화", "월식 파편 하나를 파괴해 마경의 영향력을 약화했다.", 88, "shard", "purified"));
        return s;
    }

    private static StoryScene forestTracks(StoryCampaign c) {
        StoryScene s = base("forest_tracks", "빗속의 추적", "western-copse", "서문 밖 검은자작 숲",
                "실종된 운전수와 탈취범을 추적한다",
                "세라", "태양교단 조사관", "긴장",
                "발자국은 서문 밖 숲으로 이어진다. 나뭇가지마다 검은 실이 묶여 있고, 멀리서 마차 바퀴가 돌을 긁는 소리가 들린다.",
                "저 실은 악마숭배자들이 길을 표시할 때 써요. 가까이 있습니다. 이제부터는 말보다 발을 조심해요.");
        s.choices.add(remember(choice("tracks_rescue", "희미한 신음소리부터 확인한다",
                "왼쪽에서 사람이 숨을 쉬는 소리가 납니다. 추적보다 구조가 먼저예요.",
                "구조", "보통", "ambush", 25,
                e -> { e.dTrust = 7; e.dGood = 3; e.dHp = -2; e.setFlag = "found_driver"; e.setFlagValue = "true"; }),
                "RESCUE", "살아 있는 운전수", "추적 도중 부상당한 교단 운전수를 발견했다.", 85, "rescue", "driver"));
        s.choices.add(choice("tracks_flank", "길을 우회해 습격자의 뒤를 잡는다",
                "정면으로 가면 매복에 걸립니다. 능선을 돌아 뒤를 잡죠.",
                "전술", "위험", "ambush", 35,
                e -> { e.dPower = 2; e.dInsight = 2; e.dNeutral = 2; e.setFlag = "flanked_ambush"; e.setFlagValue = "true"; }));
        s.choices.add(remember(choice("tracks_question", "세라에게 교단 내부 사정을 추궁한다",
                "싸우기 전에 묻죠. 교단 안에서 누가 이 물건을 원합니까?",
                "대화", "보통", "ambush", 20,
                e -> { e.dTrust = 2; e.dGuard = 5; e.dInsight = 3; }),
                "QUESTION", "숲에서의 추궁", c.player.name + "은 위험 속에서도 세라가 감춘 정보를 추궁했다.", 62, "sera", "question"));
        return s;
    }

    private static StoryScene ambush(StoryCampaign c) {
        StoryScene s = base("ambush", "달 없는 자들의 매복", "western-copse", "검은자작 숲 폐도",
                "악마숭배자의 매복을 돌파한다",
                "세라", "태양교단 조사관", "전투",
                "검은 실이 일제히 끊어지며 세 명의 복면인이 숲길을 막는다. 가장 뒤의 인물이 초승달 모양 단검을 들어 올리자, 나무 그림자가 살아 있는 것처럼 꿈틀거린다.",
                "하급 악마의 힘을 빌린 자들이에요. 그림자를 밟지 마요. 내가 빛을 고정할 테니 당신이 길을 열어요!");
        s.choices.add(remember(choice("ambush_guard", "세라의 앞을 지키며 정면으로 돌파한다",
                "빛을 유지하세요. 앞은 제가 막겠습니다.",
                "전투", "위험", "chapel", 30,
                e -> { e.dHp = -10; e.dPower = 3; e.dTrust = 10; e.dAffinity = 5; e.dGood = 2; }),
                "BATTLE", "등을 맡긴 첫 전투", c.player.name + "은 매복 속에서 세라의 앞을 지켰다.", 92, "sera", "battle"));
        s.choices.add(choice("ambush_trick", "젖은 검은 실을 이용해 주문진을 무너뜨린다",
                "그림자가 실을 따라 움직입니다. 매듭을 한꺼번에 끊죠.",
                "기지", "보통", "chapel", 25,
                e -> { e.dInsight = 3; e.dPower = 1; e.dTrust = 5; e.dNeutral = 2; e.dDemon = -2; }));
        s.choices.add(remember(choice("ambush_deal", "우두머리에게 파편을 찾는 이유를 묻고 거래를 제안한다",
                "우릴 죽이면 파편의 행방도 끝입니다. 먼저 거래부터 하죠.",
                "협상", "매우 위험", "chapel", 35,
                e -> { e.dGold = 12; e.dInsight = 4; e.dGuard = 12; e.dEvil = 3; e.dDemon = 2; e.setFlag = "cult_contact"; e.setFlagValue = "true"; }),
                "DEAL", "악마숭배자와의 거래", c.player.name + "은 매복한 악마숭배자에게 거래를 제안했다.", 96, "cult", "deal", "sera"));
        if (c.player.inventory.contains("월식 파편")) {
            s.choices.add(remember(choice("ambush_shard", "[월식 파편] 파편의 힘으로 그림자를 지배한다",
                    "이 힘이 누구를 주인으로 택하는지 확인해 보죠.",
                    "금단", "치명적", "chapel", 20,
                    e -> { e.dHp = -5; e.dPower = 7; e.dDemon = 6; e.dGuard = 20; e.dEvil = 6; e.setFlag = "used_shard"; e.setFlagValue = "true"; }),
                    "CORRUPTION", "월식 파편의 사용", c.player.name + "은 세라 앞에서 월식 파편의 힘을 사용했다.", 100, "shard", "corruption", "sera"));
        }
        return s;
    }

    private static StoryScene chapel(StoryCampaign c) {
        StoryScene s = base("chapel", "버려진 예배당", "abandoned-chapel", "서부 가도 폐예배당",
                "납치된 교단 전령과 배후의 단서를 확보한다",
                "세라", "태양교단 조사관", "침착",
                "매복을 벗어나자 오래전 버려진 예배당이 나타난다. 제단 아래에는 결박된 교단 전령이 있고, 뒷문 너머로 귀족 문장이 새겨진 망토가 사라진다.",
                "전령을 살리면 내부 배신자를 증언할 수 있어요. 하지만 저 망토의 주인을 놓치면 배후는 다시 숨겠죠.");
        s.choices.add(remember(choice("chapel_rescue", "세라와 함께 전령을 구한다",
                "배후는 다시 찾을 수 있습니다. 살아 있는 증인을 먼저 구하죠.",
                "구조", "보통", "oath", 40,
                e -> { e.dTrust = 12; e.dAffinity = 6; e.dGood = 5; e.dMood = 3; e.setFlag = "rescued_courier"; e.setFlagValue = "true"; }),
                "RESCUE", "교단 전령 구출", c.player.name + "은 배후 추적보다 교단 전령의 목숨을 선택했다.", 96, "sera", "courier", "rescue"));
        s.choices.add(remember(choice("chapel_chase", "망토를 쫓아 배후의 얼굴을 확인한다",
                "전령은 당신에게 맡기죠. 저 문장의 주인을 확인하고 오겠습니다.",
                "추적", "위험", "oath", 45,
                e -> { e.dHp = -5; e.dInsight = 5; e.dPower = 2; e.dTrust = -2; e.dNeutral = 3; e.setFlag = "saw_noble"; e.setFlagValue = "true"; }),
                "DISCOVERY", "귀족의 문장", c.player.name + "은 배후를 쫓아 솔라니아 고위 귀족의 문장을 확인했다.", 98, "noble", "conspiracy"));
        s.choices.add(remember(choice("chapel_bargain", "전령이 가진 비밀문서를 먼저 확보한다",
                "구하기 전에 증거부터 확보합니다. 이 문서가 사라지면 모두 헛수고가 돼요.",
                "실리", "위험", "oath", 35,
                e -> { e.dInsight = 4; e.dGold = 6; e.dGuard = 8; e.dNeutral = 2; e.setFlag = "kept_dossier"; e.setFlagValue = "true"; e.addItem = "봉인된 교단 문서"; }),
                "SECRET", "봉인된 교단 문서", c.player.name + "은 전령의 구출보다 먼저 봉인된 문서를 확보했다.", 91, "document", "secret", "sera"));
        return s;
    }

    private static StoryScene oath(StoryCampaign c) {
        StoryRelationship r = c.relationships.get(COMPANION_ID);
        String reaction = r.guard >= 45
                ? "세라는 당신을 오래 바라본다. 믿음보다는 필요 때문에 내미는 손처럼 보인다."
                : "세라는 젖은 장갑을 벗고, 흉터가 남은 오른손을 당신에게 내민다.";
        StoryScene s = base("oath", "잿빛 서약", "abandoned-chapel", "서부 가도 폐예배당",
                "앞으로 세라와 어떤 관계로 움직일지 정한다",
                "세라", "태양교단 조사관", "진지",
                reaction + " 깨진 제단 위에는 태양 문양과 월식의 그을음이 나란히 남아 있다.",
                "오늘 본 일은 시작일 뿐이에요. 교단과 제국 어딘가에 월식 마경과 손잡은 자가 있어요. 나와 함께 끝까지 추적해 줄래요?");
        s.choices.add(remember(choice("oath_companion", "세라의 동료로서 함께하겠다고 맹세한다",
                "좋아요. 당신의 빛이 닿지 않는 곳은 내가 보겠습니다. 끝까지 함께 가죠.",
                "서약", "중대", "hub", 60,
                e -> { e.dTrust = 15; e.dAffinity = 10; e.dGood = 3; e.setJob = "adventurer"; e.setJobLabel = "모험가"; e.setFlag = "ash_oath"; e.setFlagValue = "companion"; }),
                "OATH", "잿빛 서약", c.player.name + "은 세라의 동료로서 월식의 음모를 끝까지 추적하기로 맹세했다.", 100, "sera", "oath", "main_quest"));
        s.choices.add(remember(choice("oath_contract", "독립적인 모험가로 계약만 맺는다",
                "맹세는 하지 않겠습니다. 대신 계약이 이어지는 동안은 확실히 일하죠.",
                "계약", "중대", "hub", 60,
                e -> { e.dTrust = 5; e.dNeutral = 4; e.setJob = "adventurer"; e.setJobLabel = "독립 모험가"; e.setFlag = "ash_oath"; e.setFlagValue = "contract"; }),
                "CONTRACT", "장기 조사 계약", c.player.name + "은 세라와 월식 사건에 관한 장기 계약을 맺었다.", 88, "sera", "contract", "main_quest"));
        s.choices.add(remember(choice("oath_church", "태양교단에 정식 협력자로 들어간다",
                "개인의 약속만으론 부족합니다. 교단의 권한과 책임까지 받겠습니다.",
                "입단", "중대", "hub", 90,
                e -> { e.dTrust = 10; e.dEmpire = 2; e.dGood = 2; e.setJob = "priest"; e.setJobLabel = "태양교단 협력 사제"; e.setFlag = "ash_oath"; e.setFlagValue = "church"; }),
                "OATH", "태양교단의 서약", c.player.name + "은 태양교단의 협력자가 되어 월식 사건을 추적하기로 했다.", 94, "sera", "church", "main_quest"));
        return s;
    }

    private static StoryScene hub(StoryCampaign c) {
        StoryRelationship r = c.relationships.get(COMPANION_ID);
        StoryScene s = base("hub", "다음 걸음", "grey-lantern", "솔라니아 서부구 회색등불 여관",
                "세라와 다음 행동을 결정한다",
                "세라", "월식 사건 조사자", r.trust >= 40 ? "편안" : "차분",
                "며칠 뒤, 회색등불 여관의 구석방이 두 사람의 임시 거점이 된다. 벽에는 아르데르 대륙 지도와 사건을 잇는 붉은 실이 걸려 있다.",
                "급한 불은 껐지만 배후는 그대로예요. 서두르지 않아도 돼요. 다음에 무엇을 할지는 함께 정하죠.");
        s.choices.add(choice("hub_talk", "세라와 지금까지의 일을 이야기한다",
                "임무 이야기는 잠깐 미뤄두죠. 당신에 대해 더 알고 싶습니다.",
                "관계", "안전", "sera_talk", 35,
                e -> { e.dAffinity = 2; }));
        s.choices.add(choice("hub_guild", "모험가 길드의 실종 의뢰를 확인한다",
                "가만히 있을 수는 없죠. 길드에 올라온 실종 의뢰부터 봅시다.",
                "의뢰", "보통", "guild_job", 80,
                e -> {}));
        s.choices.add(choice("hub_west", "카라자드로 향하는 상단을 조사한다",
                "위조된 향 냄새가 카라자드 상단과 닿아 있습니다. 서부 가도로 가죠.",
                "여행", "위험", "west_road", 3 * 24 * 60,
                e -> {}));
        s.choices.add(choice("hub_rest", "방을 정비하고 충분히 휴식한다",
                "오늘은 쉬죠. 지친 상태로 내리는 선택은 대개 틀리니까요.",
                "휴식", "안전", "rest", 0,
                e -> {}));

        StoryChoice secret = choice("hub_secret", "세라가 숨긴 교단 기록을 함께 연다",
                "이제 그 봉인된 기록을 보여줄 때가 된 것 같습니다.",
                "핵심 사건", "중대", "secret_archive", 45,
                e -> {});
        if (r.trust < 35 && !"true".equals(c.flags.get("rescued_courier"))) {
            secret.locked = true;
            secret.lockedReason = "세라의 신뢰 35 또는 전령 구출 필요";
        }
        s.choices.add(secret);
        return s;
    }

    private static StoryScene seraTalk(StoryCampaign c) {
        String remembered = c.memories.stream()
                .filter(m -> m.active && m.tags.contains("sera"))
                .max(Comparator.comparingInt((StoryMemory m) -> m.importance).thenComparingInt(m -> m.turn))
                .map(m -> "세라는 잠시 말을 멈추고 ‘" + m.title + "’ 때의 일을 떠올린다.")
                .orElse("세라는 따뜻한 차를 두 잔 내려놓고 창밖의 빗소리에 귀를 기울인다.");
        StoryRelationship r = c.relationships.get(COMPANION_ID);
        StoryScene s = base("sera_talk", "불빛 아래의 대화", "grey-lantern", "회색등불 여관",
                "세라와 서로에 대한 이해를 깊게 한다",
                "세라", "월식 사건 조사자", r.guard > r.trust ? "조심스러움" : "부드러움",
                remembered,
                "이 일을 시작한 뒤로 누군가와 이렇게 오래 이야기한 적이 없어요. 당신은 왜 위험한 일에 계속 남아 있는 거죠?");
        s.choices.add(remember(choice("talk_honest", "자신이 원하는 것을 솔직하게 말한다",
                "세상을 구하겠다는 거창한 이유는 없습니다. 다만 내 선택을 남이 정하게 두고 싶지 않아요.",
                "진심", "안전", "hub", 45,
                e -> { e.dTrust = 8; e.dAffinity = 7; e.dGuard = -3; }),
                "BOND", "서로에게 밝힌 진심", c.player.name + "은 세라에게 위험 속에 남는 개인적인 이유를 솔직히 말했다.", 78, "sera", "bond"));
        s.choices.add(choice("talk_question", "세라가 교단을 의심하게 된 이유를 묻는다",
                "내 이야기는 했으니 당신 차례입니다. 왜 자기 교단을 의심합니까?",
                "질문", "보통", "hub", 45,
                e -> { e.dInsight = 3; e.dTrust = 4; e.setFlag = "sera_past_hint"; e.setFlagValue = "true"; }));
        s.choices.add(remember(choice("talk_distance", "사적인 대화를 피하고 임무만 확인한다",
                "우리는 임무 때문에 함께 있는 겁니다. 다음 단서만 정리하죠.",
                "거리두기", "안전", "hub", 25,
                e -> { e.dGuard = 6; e.dNeutral = 2; }),
                "IMPRESSION", "그어진 거리", c.player.name + "은 세라와의 관계를 임무로 한정했다.", 55, "sera", "distance"));
        return s;
    }

    private static StoryScene guildJob(StoryCampaign c) {
        StoryScene s = base("guild_job", "지하수로의 울음", "solania-aqueduct", "솔라니아 구 지하수로",
                "사라진 길드 조사대의 행방을 확인한다",
                "세라", "월식 사건 조사자", "경계",
                "모험가 길드의 실종 의뢰를 따라 지하수로로 내려간다. 벽에는 짐승의 발톱 자국과 월식 파편에서 보았던 은빛 가루가 함께 묻어 있다.",
                "단순한 마물 습격이 아니에요. 누군가 마물을 도시 안으로 유도했어요. 생존자를 찾되 퇴로는 잊지 마요.");
        s.choices.add(remember(choice("guild_rescue", "생존자의 횃불 신호를 따라간다",
                "오른쪽 통로에서 세 번, 두 번. 길드 구조 신호입니다. 그쪽으로 가죠.",
                "구조", "위험", "hub", 120,
                e -> { e.dHp = -6; e.dGold = 14; e.dGood = 3; e.dTrust = 5; e.dMood = 2; }),
                "RESCUE", "지하수로 생존자", "지하수로에서 실종된 길드 조사대의 생존자를 구했다.", 80, "guild", "rescue"));
        s.choices.add(choice("guild_trap", "마물이 드나든 통로를 봉쇄하고 함정을 놓는다",
                "쫓아가면 불리합니다. 여기서 길을 끊고 놈들이 돌아오길 기다리죠.",
                "전술", "보통", "hub", 150,
                e -> { e.dPower = 2; e.dGold = 10; e.dInsight = 2; e.dNeutral = 2; e.dDemon = -2; }));
        s.choices.add(remember(choice("guild_sample", "은빛 가루와 마물의 피를 몰래 채취한다",
                "증거가 필요합니다. 길드에는 말하지 말고 표본을 챙기죠.",
                "비밀 조사", "위험", "hub", 110,
                e -> { e.dInsight = 5; e.dGuard = 3; e.addItem = "오염된 은빛 표본"; }),
                "DISCOVERY", "오염된 은빛 표본", "지하수로에서 마경의 흔적이 섞인 마물 표본을 얻었다.", 86, "sample", "demon"));
        return s;
    }

    private static StoryScene westRoad(StoryCampaign c) {
        StoryScene s = base("west_road", "카라자드 상단", "sun-dune-post", "솔라니아-카라자드 국경역",
                "월식 사건과 연결된 위조 향의 유통 경로를 밝힌다",
                "나디아", "카라자드 상단주", "여유",
                "사흘 길을 지나 도착한 국경역에는 카라자드의 비단 천막과 솔라니아의 철제 마차가 뒤섞여 있다. 붉은 베일을 쓴 상단주 나디아가 찻잔을 기울인다.",
                "그 향이 우리 사막에서 왔다는 말은 절반만 맞아요. 원료는 카라자드산이지만, 완성한 자는 솔라니아 귀족이죠. 정보에는 언제나 값이 붙습니다.");
        s.choices.add(remember(choice("west_trade", "정당한 값을 치르고 거래 장부를 산다",
                "정보의 값은 치르겠습니다. 대신 장부가 진짜라는 보증도 받아야겠군요.",
                "거래", "안전", "hub", 90,
                e -> { e.dGold = -18; e.dInsight = 5; e.dNeutral = 3; e.setFlag = "trade_ledger"; e.setFlagValue = "true"; e.addItem = "카라자드 거래 장부"; }),
                "EVIDENCE", "카라자드 거래 장부", "대가를 지불하고 위조 향의 유통 경로가 적힌 장부를 얻었다.", 92, "trade", "evidence"));
        s.choices.add(choice("west_pressure", "제국의 국경 수색권을 내세워 압박한다",
                "이건 협상이 아닙니다. 제국 안에서 벌어진 악마 사건에 협조하세요.",
                "압박", "위험", "hub", 75,
                e -> { e.dInsight = 3; e.dEmpire = 2; e.dMood = -2; e.dGuard = 2; e.dNeutral = 1; }));
        s.choices.add(remember(choice("west_secret", "상단주에게 악마숭배자의 암호로 접촉한다",
                "달이 없는 밤에도 그림자는 남는다. 이 말이면 값을 달리 매길 텐데요.",
                "위장", "매우 위험", "hub", 100,
                e -> { e.dGold = 10; e.dInsight = 6; e.dDemon = 3; e.dGuard = 7; e.dEvil = 3; e.setFlag = "underworld_contact"; e.setFlagValue = "true"; }),
                "CONTACT", "국경의 암시장 연락선", c.player.name + "은 악마숭배자의 암호를 이용해 국경 암시장과 접촉했다.", 95, "underworld", "secret"));
        return s;
    }

    private static StoryScene secretArchive(StoryCampaign c) {
        String clue = "true".equals(c.flags.get("saw_noble"))
                ? "당신이 폐예배당에서 본 귀족 문장과 기록의 봉인이 정확히 일치한다."
                : "기록의 마지막 장에는 솔라니아 고위 귀족가의 봉인이 찍혀 있다.";
        StoryScene s = base("secret_archive", "봉인된 세 번째 기록", "grey-lantern", "회색등불 여관 비밀방",
                "교단 내부 배신자와 귀족의 연결을 확인한다",
                "세라", "월식 사건 조사자", "결심",
                "세라가 태양 문양의 잠금쇠를 풀자 낡은 기록 세 장이 펼쳐진다. " + clue,
                "내 스승은 이 명단을 찾은 다음 날 이단으로 몰려 사라졌어요. 그래서 누구도 믿지 못했죠. 하지만 이제 당신에게는 전부 보여줄게요.");
        s.choices.add(remember(choice("archive_share", "세라의 비밀과 증거를 함께 짊어진다",
                "이제 당신 혼자만의 비밀이 아닙니다. 다음 이름부터 함께 확인하죠.",
                "유대", "중대", "hub", 60,
                e -> { e.dTrust = 15; e.dAffinity = 12; e.dGood = 2; e.setFlag = "shared_archive"; e.setFlagValue = "true"; }),
                "BOND", "공유된 비밀", "세라는 스승의 실종과 배신자 명단을 " + c.player.name + "에게 전부 공개했다.", 100, "sera", "secret", "main_quest"));
        s.choices.add(remember(choice("archive_copy", "명단을 몰래 한 부 더 복사해 둔다",
                "증거는 하나뿐이면 사라집니다. 복사본은 서로 다른 곳에 보관하죠.",
                "대비", "보통", "hub", 70,
                e -> { e.dInsight = 4; e.dTrust = 5; e.dNeutral = 3; e.addItem = "배신자 명단 사본"; e.setFlag = "archive_copy"; e.setFlagValue = "true"; }),
                "EVIDENCE", "배신자 명단 사본", "교단과 귀족을 잇는 배신자 명단의 사본을 만들었다.", 96, "evidence", "main_quest"));
        s.choices.add(remember(choice("archive_leverage", "명단을 제국 귀족과의 거래 수단으로 삼자고 제안한다",
                "당장 폭로하면 다 숨어버립니다. 이 명단으로 먼저 움직이게 만들죠.",
                "책략", "매우 위험", "hub", 65,
                e -> { e.dInsight = 3; e.dGold = 15; e.dGuard = 10; e.dEvil = 3; e.setFlag = "archive_leverage"; e.setFlagValue = "true"; }),
                "PLAN", "명단을 이용한 책략", c.player.name + "은 배신자 명단을 정치적 거래에 이용하자고 제안했다.", 94, "sera", "politics", "main_quest"));
        return s;
    }

    private static StoryScene rest(StoryCampaign c) {
        StoryScene s = base("rest", "잠시 멈춘 밤", "grey-lantern", "회색등불 여관",
                "몸과 장비를 정비한다",
                "세라", "월식 사건 조사자", "평온",
                "창문 밖으로 늦은 비가 내린다. 지도와 문서를 접어 둔 방에는 모처럼 사건과 무관한 침묵이 흐른다.",
                "오늘 밤만큼은 아무것도 결정하지 않아도 돼요. 내일의 우리가 계속 걸을 수 있도록 쉬는 것도 일이니까.");
        s.choices.add(choice("rest_sleep", "충분히 잠들어 체력을 회복한다",
                "그럼 먼저 쉬겠습니다. 교대할 시간이 되면 깨워주세요.",
                "회복", "안전", "hub", 480,
                e -> { e.dHp = 30; e.dAffinity = 2; }));
        s.choices.add(choice("rest_train", "짧게 몸을 단련한 뒤 휴식한다",
                "잠들기 전에 동작만 몇 번 확인하죠. 같은 실수는 하지 않겠습니다.",
                "성장", "안전", "hub", 360,
                e -> { e.dHp = 15; e.dPower = 2; }));
        s.choices.add(choice("rest_notes", "사건 기록과 소지품을 정리한다",
                "기억은 틀릴 수 있습니다. 지금까지의 단서를 기록으로 고정하죠.",
                "정리", "안전", "hub", 240,
                e -> { e.dHp = 10; e.dInsight = 3; e.dTrust = 2; }));
        return s;
    }

    private static StoryScene base(String id, String title, String locationId, String locationName,
                                   String objective, String speakerName, String speakerRole, String mood,
                                   String narration, String dialogue) {
        StoryScene s = new StoryScene();
        s.id = id;
        s.title = title;
        s.locationId = locationId;
        s.locationName = locationName;
        s.objective = objective;
        s.speakerId = "세라".equals(speakerName) ? COMPANION_ID : "nadia";
        s.speakerName = speakerName;
        s.speakerRole = speakerRole;
        s.mood = mood;
        s.narration = narration;
        s.dialogue = dialogue;
        return s;
    }

    private static StoryChoice choice(String id, String text, String playerLine, String category, String risk,
                                      String nextSceneId, int timeMinutes, Consumer<StoryEffect> configure) {
        StoryChoice c = new StoryChoice();
        c.id = id;
        c.text = text;
        c.playerLine = playerLine;
        c.category = category;
        c.risk = risk;
        c.nextSceneId = nextSceneId;
        c.timeMinutes = timeMinutes;
        configure.accept(c.effect);
        return c;
    }

    private static StoryChoice remember(StoryChoice c, String type, String title, String summary,
                                        int importance, String... tags) {
        c.memoryType = type;
        c.memoryTitle = title;
        c.memorySummary = summary;
        c.memoryImportance = importance;
        c.memoryTags.addAll(java.util.List.of(tags));
        return c;
    }
}
