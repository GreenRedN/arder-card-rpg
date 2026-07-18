package com.green.fantasysim.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.domain.CycleReport;
import com.green.fantasysim.engine.EventContext;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.engine.LoreBook;

import java.util.*;

public class DummyAiClient implements AiClient {
    private static final ObjectMapper om = new ObjectMapper();
    private static final LoreBook LORE = LoreBook.loadDefault();

    @Override
    public String generateEventCardJson(EventContext ctx) {
        // Deterministic-ish ID and wording: seeded by day + tags signature
        int h = Objects.hash(ctx.day, ctx.tags.signature(), ctx.turnType.name());
        String id = "E" + Math.abs(h);

        String region = ctx.tags.get("region");
        String threat = ctx.tags.get("threat");
        String theme = ctx.tags.get("theme");
        String terrain = ctx.tags.get("terrain");
        String nation = ctx.tags.get("nation");
        String faction = ctx.tags.get("faction");
        String diff = ctx.tags.get("difficulty");

        String faithSide = ctx.tags.get("faithSide");
        String politicalAxis = ctx.tags.get("politicalAxis");
        String entryPolicy = ctx.tags.get("entryPolicy");

        String playerJob = ctx.tags.get("playerJob");
        String playerRace = ctx.tags.get("playerRace");
        String playerOrigin = ctx.tags.get("playerOrigin");

        String tradeHint = LORE.tradeHint(nation);

        String deity = ctx.tags.get("deity");
        String archdemon = ctx.tags.get("archdemon");
        String demonTier = ctx.tags.get("demonTier");
        String sin = ctx.tags.get("sin");
        String site = ctx.tags.get("site");

        String place = placeKo(region, nation, faction, terrain);

        String head = switch (threat) {
            case "monster" -> "마물";
            case "bandit" -> "도적";
            case "demon" -> "마족";
            case "war" -> "전쟁";
            case "politics" -> "정치";
            case "faith" -> "신앙";
            default -> "유적";
        };

        String title = place + "의 " + head + " 사건 (" + shortTheme(theme) + ")";
        if (!"none".equals(site)) title = "[" + siteKo(site) + "] " + title;
        if (!"none".equals(deity)) title = "[" + deityKo(deity) + "] " + title;
        if (!"none".equals(archdemon)) title = "[" + archdemonKo(archdemon) + "] " + title;

        String situation = ""
                + "장소: " + place + " / 지형: " + terrainKo(terrain) + "\n"
                + "세력: " + nationKo(nation) + " / 파벌: " + factionKo(faction) + "\n"
                + (tradeHint.isBlank() ? "" : "교류: " + tradeHint + "\n")
                + (("strict".equals(entryPolicy)) ? ("출입: " + LORE.elfEntryPolicy() + "\n") : "")
                + ((faithSide == null || "none".equals(faithSide)) ? "" : ("신앙: " + faithSideKo(faithSide) + "\n"))
                + "위협: " + head + " / 테마: " + shortTheme(theme) + " / 위험도: " + diff + "\n\n"
                + loreStamp(deity, archdemon, demonTier, sin, site)
                + flavorLine(ctx, region, threat, theme);


        List<Map<String,String>> choices = new ArrayList<>();
        choices.add(Map.of("id","GOOD", "text", goodChoiceLine(threat, theme)));
        choices.add(Map.of("id","NEUTRAL", "text", neutralChoiceLine(threat, theme)));
        choices.add(Map.of("id","EVIL", "text", evilChoiceLine(threat, theme)));

        List<String> tags = List.of(
                "region:" + region,
                "terrain:" + terrain,
                "nation:" + nation,
                "race:" + ctx.tags.get("race"),
                "faction:" + faction,
                "threat:" + threat,
                "theme:" + theme,
                "difficulty:" + diff,
                "deity:" + deity,
                "archdemon:" + archdemon,
                "demonTier:" + demonTier,
                "sin:" + sin,
                "site:" + site,
                "faithSide:" + safe(faithSide),
                "politicalAxis:" + safe(politicalAxis),
                "entryPolicy:" + safe(entryPolicy),
                "playerJob:" + safe(playerJob),
                "playerRace:" + safe(playerRace),
                "playerOrigin:" + safe(playerOrigin)
        );

        Map<String,Object> json = new LinkedHashMap<>();
        json.put("id", id);
        json.put("title", title);
        json.put("situation", situation);
        json.put("choices", choices);
        json.put("tags", tags);

        try {
            return om.writeValueAsString(json);
        } catch (Exception e) {
            // last resort
            return "{\"id\":\""+id+"\",\"title\":\""+title+"\",\"situation\":\""+escape(situation)+"\","
                    + "\"choices\":[{\"id\":\"GOOD\",\"text\":\"살린다\"},{\"id\":\"NEUTRAL\",\"text\":\"타협\"},{\"id\":\"EVIL\",\"text\":\"이익\"}],"
                    + "\"tags\":[\"region:"+region+"\",\"threat:"+threat+"\",\"theme:"+theme+"\",\"difficulty:"+diff+"\"]}";
        }
    }

    @Override
    public String generateAutoLine(EventContext ctx) {
        String region = ctx.tags.get("region");
        String theme = ctx.tags.get("theme");
        String threat = ctx.tags.get("threat");
        return switch (region) {
            case "north" -> "북부의 공기는 칼날 같다. " + autoTheme(theme) + " 속에서 " + autoThreat(threat) + "의 그림자가 스친다.";
            case "central" -> "중앙은 소문이 빠르다. " + autoTheme(theme) + "이/가 " + autoThreat(threat) + "와 얽힌다.";
            case "west" -> "서부는 거래가 전쟁이다. " + autoTheme(theme) + " 속에 " + autoThreat(threat) + "가 끼어든다.";
            case "east" -> "동부 숲은 말이 없다. " + autoTheme(theme) + "이/가 " + autoThreat(threat) + "와 맞부딪힌다.";
            default -> "남부는 사람이 많다. " + autoTheme(theme) + "이/가 " + autoThreat(threat) + "로 흔들린다.";
        };
    }

    @Override
    public String generateSummary(List<String> recentLogs, WorldState w, PlayerState p, MetaState m) {
        StringBuilder sb = new StringBuilder();
        sb.append("최근 ").append(recentLogs.size()).append("턴 요약: ");
        sb.append(trend(w)).append(" / ");
        sb.append("플레이어 ").append(p.job).append(" ").append(p.tier).append(" (강함 ").append(p.power).append(")");
        return sb.toString();
    }

    @Override
    public String generateCycleSummary(List<String> recentLogs, CycleReport r) {
        int dEmp = r.worldEnd.empire - r.worldStart.empire;
        int dDem = r.worldEnd.demon - r.worldStart.demon;
        int dCult = r.worldEnd.cult - r.worldStart.cult;
        int dMood = r.worldEnd.publicMood - r.worldStart.publicMood;

        StringBuilder sb = new StringBuilder();
        sb.append("사이클 결산: ").append(r.endingDirection).append(" 방향으로 굳어졌다.\n");
        sb.append("제국 ").append(sign(dEmp)).append(", 마족 ").append(sign(dDem)).append(", 교단 ").append(sign(dCult)).append(", 민심 ").append(sign(dMood)).append(".\n");
        if (!r.majorChoices.isEmpty()) {
            sb.append("가장 큰 선택: ").append(r.majorChoices.get(0)).append("\n");
        }
        sb.append("다음 사이클은 같은 세계의 다른 단면이다. (메타는 누적)");
        return sb.toString();
    }


    private static String siteKo(String site) {
        if (site == null || "none".equals(site)) return "";
        return LORE.site(site);
    }

    private static String deityKo(String deity) {
        if (deity == null || "none".equals(deity)) return "무신";
        if ("sun".equals(deity)) return LORE.overgod();
        return LORE.minorGod(deity);
    }

    private static String demonTierKo(String tier) {
        return switch (tier) {
            case "king" -> "마왕급";
            case "high" -> "고위마족급";
            case "sin" -> "7대 죄악급";
            case "mid" -> "중급마족";
            case "low" -> "하급마족/마물";
            default -> "불명";
        };
    }

    private static String archdemonKo(String archdemon) {
        if (archdemon == null || "none".equals(archdemon)) return "마계의 그림자";
        if ("moon_king".equals(archdemon)) return LORE.demonKing();
        return LORE.archdemon(archdemon);
    }

    private static String nationKo(String nation) {
        if (nation == null) return LORE.nation("none");
        return LORE.nation(nation);
    }

    private static String factionKo(String faction) {
        if (faction == null) return LORE.faction("neutral");
        // Faith-side "church" is also a faction key, but its full institution name is shown elsewhere.
        return LORE.faction(faction);
    }

    private static String faithSideKo(String faithSide) {
        if (faithSide == null || "none".equals(faithSide)) return "없음";
        return switch (faithSide) {
            case "church" -> LORE.churchName();
            case "demon_worshippers" -> LORE.demonWorshippersName();
            default -> faithSide;
        };
    }

    private static String terrainKo(String terrain) {
        if (terrain == null) return "평야";
        return switch (terrain) {
            case "mountain" -> "산악";
            case "snowfield" -> "설원";
            case "forest" -> "숲";
            case "plains" -> "평야";
            case "desert" -> "사막";
            case "highland" -> "고원";
            default -> terrain;
        };
    }

    private static String placeKo(String region, String nation, String faction, String terrain) {
        if ("north".equals(region)) {
            if ("dwarf_guild".equals(faction)) return "북부 산맥의 " + LORE.dwarfGuildsName();
            if ("beast_anti".equals(faction) || "beast_pro".equals(faction)) return "북부 설원 수인 부족지";
            if ("demon_worshippers".equals(faction)) return "북부 설원, " + LORE.demonWorshippersName() + " 은신처";
            return "북부 설원 변두리";
        }
        if ("east".equals(region)) return nationKo(nation) + " 국경 숲 (관문)";
        if ("west".equals(region)) return nationKo(nation) + " 오아시스 변경";
        if ("central".equals(region)) return nationKo(nation) + " 중앙로";
        // south
        return nationKo(nation) + ("south_union".equals(nation) ? " 항구도시" : " 남부 평야");
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "none" : s;
    }

    private static String sinKo(String sin) {
        if (sin == null || "none".equals(sin)) return "무명";
        return LORE.sin(sin);
    }

    private static String loreStamp(String deity, String archdemon, String demonTier, String sin, String site) {
        StringBuilder sb = new StringBuilder();
        if (site != null && !"none".equals(site)) sb.append("무대: ").append(siteKo(site)).append("\n");
        if (deity != null && !"none".equals(deity)) sb.append("신앙: ").append(deityKo(deity)).append("\n");
        if (archdemon != null && !"none".equals(archdemon)) {
            sb.append("마계: ").append(archdemonKo(archdemon));
            if (demonTier != null && !"none".equals(demonTier)) sb.append(" (").append(demonTierKo(demonTier)).append(")");
            if (sin != null && !"none".equals(sin)) sb.append(" / 죄악 ").append(sinKo(sin));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private static String shortTheme(String theme) {
        return switch (theme) {
            case "survival" -> "생존";
            case "famine" -> "고갈";
            case "order" -> "질서";
            case "flow" -> "흐름";
            case "life" -> "생명";
            case "wealth" -> "풍요";
            case "end" -> "종언";
            default -> "정체";
        };
    }

    private static String flavorLine(EventContext ctx, String region, String threat, String theme) {
        String deity = ctx.tags.get("deity");
        String archdemon = ctx.tags.get("archdemon");
        String demonTier = ctx.tags.get("demonTier");
        String sin = ctx.tags.get("sin");
        String site = ctx.tags.get("site");

        String nation = ctx.tags.get("nation");
        String faction = ctx.tags.get("faction");
        String faithSide = ctx.tags.get("faithSide");
        String politicalAxis = ctx.tags.get("politicalAxis");
        String entryPolicy = ctx.tags.get("entryPolicy");
        String playerOrigin = ctx.tags.get("playerOrigin");

        StringBuilder sb = new StringBuilder();

        // Elf kingdom: strict entry gate
        if ("east".equals(region) && "strict".equals(entryPolicy)) {
            sb.append("관문은 ").append(LORE.elfEntryPolicy()).append(" — 숲은 열려 있어도 길은 닫혀 있다. ");
        }

        // Site flavor
        if (site != null && !"none".equals(site)) {
            if ("ma_gyeong".equals(site)) sb.append("마경의 서리가 숨결을 얼린다. ");
            else if ("cathedral".equals(site)) sb.append(LORE.churchName()).append("의 종소리가 멀리까지 번진다. ");
            else if ("ruins_gate".equals(site)) sb.append("유적 관문이 미세하게 떨린다. ");
        }

        // Demon / hell activity
        if ("demon".equals(threat) || (archdemon != null && !"none".equals(archdemon))) {
            sb.append(archdemonKo(archdemon)).append("의 징후가 번진다");
            if (demonTier != null && !"none".equals(demonTier)) sb.append(" (").append(demonTierKo(demonTier)).append(")");
            sb.append(". ");
            if ("noble".equals(playerOrigin) || "royal".equals(playerOrigin)) {
                sb.append("귀족의 연회장엔 '힘'과 '부'를 약속하는 속삭임이 스민다. ");
            } else {
                sb.append("하급 악마는 칼보다 말로 사람을 무너뜨린다. ");
            }
            if (sin != null && !"none".equals(sin)) sb.append("사람들의 눈빛엔 ").append(sinKo(sin)).append("이(가) 비친다. ");
            sb.append("마물은 파괴하고, 악마는 조종한다.");
            return sb.toString();
        }

        // Faith / church vs demon worshippers
        if ("faith".equals(threat)) {
            if ("demon_worshippers".equals(faithSide)) {
                sb.append(LORE.demonWorshippersName()).append("의 비밀 의식이 '달'을 부른다. ");
                sb.append("북부에선 누구도 쉽게 손을 내밀지 않는다. ");
            } else {
                sb.append(LORE.churchName()).append("의 성가가 울린다. ");
                if ("north".equals(region)) sb.append(LORE.churchNorthDispatch()).append(". ");
            }
            if (deity != null && !"none".equals(deity)) sb.append(deityKo(deity)).append("의 이름이 기도로 반복된다. ");
            sb.append("믿음은 생명일 수도, 종언일 수도 있다.");
            return sb.toString();
        }

        // Ruins
        if ("ruins".equals(threat)) {
            sb.append("돌에 새겨진 문장은 오래된 경고처럼 읽힌다. 무엇이 질서를 무너뜨렸는지, 아직은 모른다.");
            return sb.toString();
        }

        // Politics / war
        if ("politics".equals(threat) || "war".equals(threat)) {
            if (politicalAxis != null && !"none".equals(politicalAxis)) {
                sb.append(politicalAxisKo(politicalAxis)).append(" ");
                LoreBook.Relation rel = relationHint(nation, politicalAxis);
                if (rel != null && rel.notes != null && !rel.notes.isBlank()) sb.append(rel.notes);
            } else {
                sb.append("사람들의 말이 칼이 된다. ");
            }
            return sb.toString();
        }

        if ("bandit".equals(threat)) {
            sb.append("약한 고리는 항상 노려진다. 풍요는 미끼가 되고, 고갈은 현실이 된다.");
            return sb.toString();
        }

        // Monster / default
        if ("north".equals(region) && "beast_pro".equals(faction)) {
            sb.append("설원에선 '힘'이 정의로 착각되기 쉽다. 불신은 오래된 상처에서 태어난다. ");
        }
        sb.append("바람은 이상하게 차갑다. 오늘의 '").append(shortTheme(theme)).append("'은(는) 우연이 아닐지도 모른다.");
        return sb.toString();
    }

    private static LoreBook.Relation relationHint(String nation, String axis) {
        if (nation == null) return null;
        return switch (axis) {
            case "empire_desert_tension" -> LORE.relationBetween("empire","desert_kingdom");
            case "empire_union_intrigue" -> LORE.relationBetween("empire","south_union");
            case "desert_union_proxy" -> LORE.relationBetween("desert_kingdom","south_union");
            case "south_union_border" -> LORE.relationBetween("south_major","south_union");
            case "empire_south_neutral" -> LORE.relationBetween("empire","south_major");
            default -> null;
        };
    }

    private static String politicalAxisKo(String axis) {
        if (axis == null) return "";
        return switch (axis) {
            case "empire_desert_tension" -> "서부 국경엔 언제든 불씨가 있다.";
            case "empire_union_intrigue" -> "연합의 궁정엔 친제국파와 반제국파가 맞선다.";
            case "desert_union_proxy" -> "사막의 금화가 연합의 당쟁을 흔든다.";
            case "south_union_border" -> "남부 국경은 불편한 침묵으로 이어진다.";
            case "empire_south_neutral" -> "제국과 남부는 칼을 뽑지 않은 중립을 지킨다.";
            default -> "";
        };
    }

    private static String goodChoiceLine(String threat, String theme) {
        return switch (threat) {
            case "monster" -> "마을을 지킨다. 부상은 감수하되, 공포를 꺾는다.";
            case "bandit" -> "약탈로 잃은 것을 되찾고, 사형 대신 갱생을 택한다.";
            case "demon" -> "봉인을 강화한다. 대가를 치르더라도 균열을 닫는다.";
            case "war" -> "전장의 민간인을 우선 대피시킨다. 승리보다 생명을 택한다.";
            case "politics" -> "공정한 절차를 강제한다. 거래를 끊고 원칙을 세운다.";
            case "faith" -> "치유와 구호에 모든 자원을 쓴다. 믿음을 살린다.";
            default -> "유적을 정화한다. 지식을 나눠 위험을 줄인다.";
        };
    }

    private static String neutralChoiceLine(String threat, String theme) {
        return switch (threat) {
            case "monster" -> "우회로를 찾는다. 피해를 줄이되, 근본 해결은 미룬다.";
            case "bandit" -> "거래로 시간을 번다. 손해를 줄이고 상황을 관망한다.";
            case "demon" -> "임시 봉인을 친다. 당장은 막지만 후폭풍이 남는다.";
            case "war" -> "중립을 지킨다. 작은 이익과 안전을 맞바꾼다.";
            case "politics" -> "타협안을 만든다. 불만은 남지만 폭발은 막는다.";
            case "faith" -> "의식을 최소화한다. 신앙과 현실 사이 균형을 잡는다.";
            default -> "유적을 봉인한다. 연구는 중단하지만 위험도 억제한다.";
        };
    }

    private static String evilChoiceLine(String threat, String theme) {
        return switch (threat) {
            case "monster" -> "미끼를 던진다. 누군가가 죽어야 모두가 산다.";
            case "bandit" -> "도적을 이용한다. 약탈의 일부를 받아내고 뒤를 봐준다.";
            case "demon" -> "금지된 계약을 맺는다. 힘과 대가를 맞바꾼다.";
            case "war" -> "전쟁을 키워 이익을 챙긴다. 피를 계산한다.";
            case "politics" -> "협박과 공작으로 판을 뒤집는다. 승자가 정의다.";
            case "faith" -> "신앙을 도구로 삼는다. 공포를 이용해 군중을 조종한다.";
            default -> "유적의 힘을 독점한다. 위험은 남지만 강해진다.";
        };
    }

    private static String autoTheme(String theme) {
        return shortTheme(theme) + "의 기류";
    }
    private static String autoThreat(String threat) {
        return switch (threat) {
            case "monster" -> "마물";
            case "bandit" -> "도적";
            case "demon" -> "마족";
            case "war" -> "전쟁";
            case "politics" -> "정치";
            case "faith" -> "신앙";
            default -> "유적";
        };
    }

    private static String trend(WorldState w) {
        if (w.demon >= 80) return "마족 압력이 치명적";
        if (w.empire <= 30) return "제국이 흔들림";
        if (w.publicMood <= 25) return "민심이 붕괴 직전";
        if (w.cult <= 25) return "교단이 약해짐";
        if (w.empire >= 70 && w.publicMood >= 70) return "질서가 강화";
        return "균형은 불안정";
    }

    private static String sign(int d) { return (d>=0?"+":"") + d; }
    private static String escape(String s) { return s.replace("\\","\\\\").replace("\n","\\n").replace("\"","\\\""); }
}
