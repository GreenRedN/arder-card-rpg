package com.green.fantasysim.engine;

import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.util.Rand;

/**
 * Picks tags within the allowed world-lore ontology.
 * - Region first, then terrain/nation/race/faction.
 * - Threat/theme are slightly biased by current WorldState and PlayerState.
 */
public class EventPicker {

    public TagSet pick(Rand rand, WorldState world, PlayerState player) {
        TagSet t = new TagSet();

        // 0) region
        int[] regionW = new int[]{20, 25, 18, 12, 25}; // north, central, west, east, south

        // world bias
        if (world.demon >= 70) regionW[0] += 5;              // north gets scarier
        if (world.empire <= 30) regionW[4] += 4;             // south gets noisy
        if (world.publicMood <= 25) regionW[2] += 3;         // west bandits/war
        if (world.cult >= 70) regionW[1] += 3;               // central church/empire politics

        String region = TagOntology.REGIONS.get(rand.weightedIndex(regionW));
        t.put("region", region);

                // 1) region-specific static tags
        switch (region) {
            case "north" -> {
                t.put("terrain", pick(rand, new String[]{"mountain","snowfield"}, new int[]{55,45}));
                t.put("nation", "none");

                String race = pick(rand, new String[]{"human","beast","dwarf"}, new int[]{45,35,20});
                t.put("race", race);

                String faction;
                if ("beast".equals(race)) {
                    // 반악마파가 다수, 친악마파는 소수
                    faction = pick(rand, new String[]{"beast_anti","beast_pro"}, new int[]{70,30});
                } else if ("dwarf".equals(race)) {
                    faction = "dwarf_guild";
                } else {
                    // 인간: 악마숭배자는 북부에서만 주로 출몰
                    int dw = (world.demon >= 65) ? 35 : 18;
                    faction = pick(rand, new String[]{"neutral","demon_worshippers"}, new int[]{100-dw, dw});
                }
                t.put("faction", faction);
            }
            case "central" -> {
                t.put("terrain", pick(rand, new String[]{"mountain","forest","plains"}, new int[]{25,35,40}));
                t.put("nation", "empire");
                t.put("race", pick(rand, new String[]{"human","dwarf"}, new int[]{75,25}));
                // 교단은 제국권에서 영향력이 강함
                t.put("faction", pick(rand, new String[]{"empire","church"}, new int[]{65,35}));
            }
            case "west" -> {
                t.put("terrain", pick(rand, new String[]{"desert","highland"}, new int[]{70,30}));
                t.put("nation", "desert_kingdom");
                t.put("race", pick(rand, new String[]{"human","beast","dwarf"}, new int[]{60,25,15}));
                t.put("faction", pick(rand, new String[]{"neutral","church"}, new int[]{75,25}));
            }
            case "east" -> {
                t.put("terrain","forest");
                t.put("nation","elf_kingdom");
                String race = pick(rand, new String[]{"elf","human"}, new int[]{70,30});
                t.put("race", race);
                // 엘프 왕국은 폐쇄적이며 국경수비대가 주요 행정력
                t.put("faction","elf_guard");
            }
            default -> { // south
                t.put("terrain","plains");
                t.put("nation", pick(rand, new String[]{"south_union","south_major"}, new int[]{65,35}));
                t.put("race", pick(rand, new String[]{"human","dwarf"}, new int[]{80,20}));
                t.put("faction", pick(rand, new String[]{"empire","neutral","church"}, new int[]{25,55,20}));
            }
        }

// 2) threat/theme: base by region
        int[] threatW = switch (region) {
            case "north" -> new int[]{50, 0, 18, 0, 0, 7, 25};   // monster, bandit, demon, war, politics, faith, ruins
            case "central" -> new int[]{15, 20, 10, 10, 30, 10, 5};
            case "west" -> new int[]{20, 30, 5, 25, 5, 5, 10};
            case "east" -> new int[]{35, 10, 10, 5, 20, 5, 15};
            default -> new int[]{10, 15, 8, 12, 30, 15, 10};
        };

        // world bias into threats
        if (world.demon >= 70) threatW[2] += 12;     // demon
        if (world.publicMood <= 25) threatW[1] += 10; // bandit
        if (world.empire <= 30) { threatW[4] += 8; threatW[3] += 6; } // politics + war
        if (world.cult >= 70) threatW[5] += 8;       // faith
        if (world.cult <= 30) threatW[6] += 4;       // ruins (lost wards)
        // player bias
        if ("knight".equals(player.job)) { threatW[3] += 6; threatW[0] += 6; }
        if ("mage".equals(player.job))   { threatW[6] += 6; threatW[2] += 6; }
        if ("priest".equals(player.job)) { threatW[5] += 8; threatW[2] += 4; }
        if ("paladin".equals(player.job)) { threatW[5] += 8; threatW[3] += 6; threatW[0] += 4; }
        if ("adventurer".equals(player.job)) { threatW[1] += 4; threatW[0] += 4; }

        String threat = TagOntology.THREATS.get(rand.weightedIndex(threatW));
        t.put("threat", threat);

        int[] themeW = switch (threat) {
            case "monster" -> new int[]{45, 10, 8, 12, 10, 5, 5, 5};   // survival heavy
            case "bandit"  -> new int[]{20, 10, 10, 10, 10, 25, 5, 10}; // wealth/order
            case "demon"   -> new int[]{15, 5, 10, 10, 5, 5, 25, 25};  // end/decay
            case "war"     -> new int[]{15, 10, 20, 10, 10, 10, 10, 15}; // order/decay
            case "politics"-> new int[]{10, 5, 35, 15, 10, 15, 5, 5};   // order/flow/wealth
            case "faith"   -> new int[]{10, 5, 25, 10, 25, 5, 10, 10};  // life/order/end
            default        -> new int[]{15, 5, 10, 20, 10, 10, 15, 15};  // ruins: flow/end/decay
        };

        // world bias into themes
        if (world.demon >= 75) { themeW[6] += 8; themeW[7] += 8; } // end+decay
        if (world.publicMood <= 25) { themeW[1] += 10; themeW[0] += 4; } // famine + survival
        if (world.empire >= 70) themeW[2] += 6; // order
        if (world.cult >= 70) themeW[4] += 6; // life

        String theme = TagOntology.THEMES.get(rand.weightedIndex(themeW));
        t.put("theme", theme);

                // 2.4) extra world-consistency tags (narrative constraints for AI)
        String faithSide = "none";
        if ("faith".equals(threat)) {
            if ("north".equals(region) || "demon_worshippers".equals(player.faith) || "demon_worshippers".equals(t.get("faction"))) faithSide = "demon_worshippers";
            else faithSide = "church";
        }

        String politicalAxis = "none";
        if ("politics".equals(threat) || "war".equals(threat)) {
            String nation = t.get("nation");
            if ("desert_kingdom".equals(nation)) politicalAxis = "empire_desert_tension";
            else if ("south_union".equals(nation)) politicalAxis = ("war".equals(threat) ? "south_union_border" : "empire_union_intrigue");
            else if ("south_major".equals(nation)) politicalAxis = "south_union_border";
            else if ("empire".equals(nation)) politicalAxis = ("politics".equals(threat) ? "empire_union_intrigue" : "empire_desert_tension");
        }

        String entryPolicy = "none";
        if ("east".equals(region)) entryPolicy = "strict"; // 엘프 왕국 관문 심사

        t.put("faithSide", faithSide);
        t.put("politicalAxis", politicalAxis);
        t.put("entryPolicy", entryPolicy);

// 2.5) lore anchors: gods/demon hierarchy/hell-front (ma_gyeong)
        boolean demonRelated = "demon".equals(threat)
                || ("faith".equals(threat) && "demon_worshippers".equals(player.faith))
                || ("ruins".equals(threat) && world.demon >= 75);

        String demonTier = "none";
        String archdemon = "none";
        String sin = "none";
        if (demonRelated) {
            int d = world.demon;
            if (d >= 92) demonTier = "king";
            else if (d >= 82) demonTier = "high";
            else if (d >= 72) demonTier = "sin";
            else if (d >= 62) demonTier = "mid";
            else demonTier = "low";

            if ("king".equals(demonTier)) archdemon = "moon_king";
            else if ("end".equals(theme)) archdemon = "end";
            else if ("famine".equals(theme)) archdemon = "famine";
            else if ("decay".equals(theme)) archdemon = "stagnation";
            else archdemon = "distortion";

            if ("sin".equals(demonTier)) {
                // rough mapping: keep sins visible & somewhat tied to situations
                sin = switch (threat) {
                    case "war" -> "wrath";
                    case "bandit" -> "greed";
                    case "politics" -> "pride";
                    case "ruins" -> "envy";
                    default -> switch (theme) {
                        case "wealth" -> "greed";
                        case "life" -> "lust";
                        case "order" -> "pride";
                        case "flow" -> "envy";
                        case "survival" -> "gluttony";
                        case "famine" -> "gluttony";
                        case "end" -> "sloth";
                        default -> "wrath";
                    };
                };
            }
        }

        String deity = "none";
        if ("faith".equals(threat) && !"demon_worshippers".equals(player.faith)) {
            // 교단(태양성 교단) 쪽 의식은 보통 4대 신에, 경우에 따라 주신(태양신)으로 앵커링
            deity = switch (theme) {
                case "life" -> "life";
                case "wealth" -> "wealth";
                case "order" -> "order";
                case "flow" -> "flow";
                default -> "sun";
            };
        }

        String site = "none";
        if ("north".equals(region) && "snowfield".equals(t.get("terrain")) && demonRelated && world.demon >= 70) site = "ma_gyeong";
        else if ("central".equals(region) && "faith".equals(threat) && "church".equals(t.get("faction"))) site = "cathedral";
        else if ("ruins".equals(threat) && world.demon >= 70) site = "ruins_gate";

        // Job-route onboarding sites (low probability):
        // player starts as a commoner (job=none) and can "bump into" an institution which triggers a YES/NO offer.
        if ("none".equals(site) && "none".equals(player.job)) {
            int r = rand.nextInt(100);
            if (r < 12) site = "adventurer_guild";        // 길드
            else if (r < 20) site = "mage_tower";         // 마탑
            else if (r < 26) site = "knight_order";       // 기사단
            else if (r < 30) site = "cathedral";          // 교단
        }

        // Cross-institution encounter: priest may be invited to the knight order (to unlock paladin route).
        if ("none".equals(site) && "priest".equals(player.job)) {
            // keep it in civilized areas
            if ("central".equals(region) || "south".equals(region) || "west".equals(region)) {
                int r = rand.nextInt(100);
                if (r < 8) site = "knight_order";
            }
        }

        t.put("deity", deity);
        t.put("archdemon", archdemon);
        t.put("demonTier", demonTier);
        t.put("sin", sin);
        t.put("site", site);

        // 3) difficulty 20..80, with biases
        int base = 20 + rand.nextInt(61);
        if ("north".equals(region)) base += 8;
        if ("demon".equals(threat)) base += 10;
        if (world.demon >= 80) base += 6;
        if (world.publicMood <= 20) base += 4;
        base = Math.max(10, Math.min(95, base));
        t.put("difficulty", String.valueOf(base));

        // also attach player side for AI text, but kept within ontology keys
        t.put("playerJob", player.job);
        t.put("playerRace", player.race);
        t.put("playerOrigin", player.origin);
        return t;
    }

    private static String pick(Rand rand, String[] vals, int[] w) {
        return vals[rand.weightedIndex(w)];
    }
}
