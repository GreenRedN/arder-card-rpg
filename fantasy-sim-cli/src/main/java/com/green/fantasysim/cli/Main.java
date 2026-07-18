package com.green.fantasysim.cli;

import com.green.fantasysim.engine.*;
import com.green.fantasysim.domain.*;

import java.util.Locale;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Locale.setDefault(Locale.KOREA);

        System.out.println("=== Fantasy Sim v2 (CLI) ===");
        System.out.println("30일 사이클 / 이벤트 + 선택 / WorldState 변화 / 사이클 종료 리포트");

        Config cfg = ConfigLoader.loadDefault();

        MetaState meta = null;
        GameSession session = null;
        SessionEngine engine = new SessionEngine();

        // first boot
        PlayerState player = promptNewPlayer();
        meta = SessionFactory.createMetaFromScratch(player);
        session = SessionFactory.newSession(cfg, player, meta);

        while (true) {
            TurnOutcome out = engine.next(session);
            renderOutcome(out);

            if (out.needsChoice) {
                String choiceId = promptChoice(out.eventCard);
                TurnOutcome chosen = engine.choose(session, choiceId);
                System.out.println("=> 결과: " + (chosen.appliedEffectSummary == null ? "-" : chosen.appliedEffectSummary));
                printStatsLine(chosen);
                if (chosen.ended) {
                    System.out.println(ReportBuilder.formatForCli(chosen.cycleReport));
                    int act = promptAfterEnd(session.player);
                    if (act == 0) break;
                    if (act == 3) {
                        // full reset
                        player = promptNewPlayer();
                        meta = SessionFactory.createMetaFromScratch(player);
                        session = SessionFactory.newSession(cfg, player, meta);
                        continue;
                    }
                    if (act == 2) {
                        // rebirth
                        try {
                            session = SessionFactory.rebirth(session);
                        } catch (Exception ex) {
                            System.out.println("환생 불가: " + ex.getMessage());
                            // fallback to new cycle
                            player = promptNewPlayer();
                            session = SessionFactory.newSession(cfg, player, meta);
                        }
                        continue;
                    }
                    if (act == 1) {
                        // next cycle, new character (meta 유지)
                        player = promptNewPlayer();
                        session = SessionFactory.newSession(cfg, player, meta);
                        session.cycleNo += 1;
                        continue;
                    }
                }
            } else {
                printStatsLine(out);
                if (out.ended) {
                    System.out.println(ReportBuilder.formatForCli(out.cycleReport));
                    int act = promptAfterEnd(session.player);
                    if (act == 0) break;
                    if (act == 3) {
                        player = promptNewPlayer();
                        meta = SessionFactory.createMetaFromScratch(player);
                        session = SessionFactory.newSession(cfg, player, meta);
                        continue;
                    }
                    if (act == 2) {
                        try {
                            session = SessionFactory.rebirth(session);
                        } catch (Exception ex) {
                            System.out.println("환생 불가: " + ex.getMessage());
                            player = promptNewPlayer();
                            session = SessionFactory.newSession(cfg, player, meta);
                        }
                        continue;
                    }
                    if (act == 1) {
                        player = promptNewPlayer();
                        session = SessionFactory.newSession(cfg, player, meta);
                        session.cycleNo += 1;
                    }
                }
            }
        }

        System.out.println("\n종료.");
    }

    private static void renderOutcome(TurnOutcome out) {
        System.out.println("\n------------------------------");
        System.out.println("Day " + out.day + " / " + out.turnType);
        if (out.turnType == TurnType.SUMMARY) {
            System.out.println(out.summaryText);
            return;
        }
        if (out.turnType == TurnType.AUTO) {
            System.out.println(out.autoLine);
            if (out.appliedEffectSummary != null && !out.appliedEffectSummary.isBlank())
                System.out.println("효과: " + out.appliedEffectSummary);
            return;
        }
        if (out.needsChoice && out.eventCard != null) {
            System.out.println("[EVENT] " + out.eventCard.title);
            System.out.println(out.eventCard.situation);
            System.out.println("\n선택:");
            for (int i=0;i<out.eventCard.choices.size();i++) {
                Choice c = out.eventCard.choices.get(i);
                System.out.println((i+1) + ") " + c.id + " - " + c.text);
            }
        }
    }

    private static PlayerState promptNewPlayer() {
        System.out.println("\n[캐릭터 생성]");

        // Start flow: race first, then optional nameless, then name (only if not nameless)
        String race = pick("종족", new String[]{"human","elf","beast","dwarf"},
                new String[]{"인간","엘프","수인","드워프"});

        boolean nameless = askYesNo("이름 없는 자로 시작할까? (환생 불가)");
        if (nameless) {
            PlayerState p = SessionFactory.createStartingPlayer(race, "", true);
            System.out.println("생성 완료: " + p.name + " / " + p.race + " (출신 " + p.origin + ", 직업 " + p.job + ")");
            return p;
        }

        String name;
        while (true) {
            System.out.print("이름 입력> ");
            name = sc.nextLine().trim();
            if (!name.isBlank()) break;
            System.out.println("이름은 비울 수 없다.");
        }

        PlayerState p = SessionFactory.createStartingPlayer(race, name, false);
        System.out.println("생성 완료: " + p.name + " / " + p.race + " (출신 평민, 직업은 플레이 중 결정)");
        return p;
    }

    private static String pick(String label, String[] keys, String[] names) {
        while (true) {
            System.out.println("- " + label + " 선택:");
            for (int i=0;i<keys.length;i++) {
                System.out.println("  " + (i+1) + ") " + names[i] + " [" + keys[i] + "]");
            }
            System.out.print("> ");
            String in = sc.nextLine().trim();
            int idx = parseIndex(in, keys.length);
            if (idx >= 0) return keys[idx];
            System.out.println("다시 입력.");
        }
    }

    private static int parseIndex(String in, int n) {
        try {
            int x = Integer.parseInt(in);
            if (x >= 1 && x <= n) return x-1;
        } catch (Exception ignored) {}
        return -1;
    }

    private static boolean askYesNo(String q) {
        while (true) {
            System.out.print(q + " (y/n) > ");
            String in = sc.nextLine().trim().toLowerCase();
            if (in.equals("y") || in.equals("yes") || in.equals("예")) return true;
            if (in.equals("n") || in.equals("no") || in.equals("아니오") || in.equals("아니요")) return false;
            System.out.println("y 또는 n 으로 입력.");
        }
    }

    private static String promptChoice(EventCard card) {
        while (true) {
            System.out.print("> 선택 (번호/ID, status, q): ");
            String in = sc.nextLine().trim();

            if (in.equalsIgnoreCase("Q")) System.exit(0);
            if (in.equalsIgnoreCase("STATUS")) {
                System.out.println("현재 상태는 바로 아래 줄에 출력된다. 계속 선택해.");
                continue;
            }

            // numeric
            int idx = parseIndex(in, card.choices.size());
            if (idx >= 0) return card.choices.get(idx).id;

            // id
            String up = in.toUpperCase();
            for (Choice c : card.choices) {
                if (up.equals(c.id.toUpperCase())) return c.id;
            }

            System.out.println("잘못된 입력.");
        }
    }

    private static void printStatsLine(TurnOutcome out) {
        if (out == null || out.world == null || out.player == null) return;
        System.out.println("World: 제국 " + out.world.empire + " / 마족 " + out.world.demon + " / 교단 " + out.world.cult + " / 민심 " + out.world.publicMood);
        System.out.println("Player: " + out.player.name + " / " + out.player.race + " / 출신 " + out.player.origin + "(" + out.player.originDetail + ")"
                + " / HP " + out.player.hp + " / G " + out.player.gold + " / 강함 " + out.player.power
                + " / " + out.player.job + " " + out.player.tier);
        System.out.println("Meta: 고난 " + out.meta.hardship + " / 통찰(제국) " + out.meta.insightEmpire + " / 통찰(마족) " + out.meta.insightDemon + " / 통찰(교단) " + out.meta.insightCult + " / 메타힘 " + out.meta.metaPower);
    }

    private static int promptAfterEnd(PlayerState p) {
        boolean canRebirth = p != null && !"nameless".equals(p.origin);
        System.out.println("[다음 행동]");
        System.out.println("1) 다음 사이클(새 캐릭터 선택, 메타 유지)");
        if (canRebirth) System.out.println("2) 환생(랜덤 출생/랜덤 이름, 메타 유지, 약간의 강함 이월)");
        else System.out.println("2) 환생(불가: 무명의 자)");
        System.out.println("3) 완전 초기화(메타 포함 리셋)");
        System.out.println("0) 종료");
        while (true) {
            System.out.print("> ");
            String in = sc.nextLine().trim();
            try {
                int x = Integer.parseInt(in);
                if (x == 2 && !canRebirth) {
                    System.out.println("환생 불가. 1 또는 3 또는 0.");
                    continue;
                }
                if (x==0 || x==1 || x==2 || x==3) return x;
            } catch (Exception ignored) {}
            System.out.println("다시 입력.");
        }
    }
}
