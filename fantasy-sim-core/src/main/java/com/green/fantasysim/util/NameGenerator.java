package com.green.fantasysim.util;

/**
 * Simple deterministic name generator for reincarnation.
 * This is intentionally lightweight: no external files, no localization dependencies.
 */
public final class NameGenerator {
    private NameGenerator() {}

    private static final String[] HUMAN = {"가렌", "리안", "에드릭", "카일", "로웬", "세린", "마르코", "엘렌", "벨로", "하르트"};
    private static final String[] ELF = {"실리안", "엘로윈", "아리아", "린델", "세레나", "페이르", "미리엘", "아르벨", "루시엔", "나이아"};
    private static final String[] BEAST = {"라크", "오르마", "브라운", "시라", "토르", "네스", "바크", "루마", "칸", "우르"};
    private static final String[] DWARF = {"브룸", "토리", "그림", "드왈", "카즈", "바린", "스톤", "루닉", "하비", "모르"};

    private static final String[] HUMAN_SURN = {"아르덴", "발렌", "로데르", "카이렌", "하이든", "모르간", "레이븐", "솔라", "브라이트"};
    private static final String[] ELF_SURN = {"실바", "달그로브", "루미나", "바람잎", "은빛가지", "별샘", "이슬숲"};
    private static final String[] BEAST_SURN = {"강송곳니", "붉은갈기", "흰발톱", "바위등", "밤눈", "회색꼬리"};
    private static final String[] DWARF_SURN = {"아이언", "스톤해머", "골드브로", "다크포지", "브론즈비어드"};

    public static String randomName(Rand rand, String race) {
        if (rand == null) throw new IllegalArgumentException("rand null");
        String r = (race == null) ? "human" : race;

        String given;
        String surn;
        switch (r) {
            case "elf" -> {
                given = pick(rand, ELF);
                surn = pick(rand, ELF_SURN);
            }
            case "beast" -> {
                given = pick(rand, BEAST);
                surn = pick(rand, BEAST_SURN);
            }
            case "dwarf" -> {
                given = pick(rand, DWARF);
                surn = pick(rand, DWARF_SURN);
            }
            default -> {
                given = pick(rand, HUMAN);
                surn = pick(rand, HUMAN_SURN);
            }
        }

        // 60%: 성+이름 / 40%: 이름만
        return (rand.nextInt(100) < 60) ? (surn + " " + given) : given;
    }

    private static String pick(Rand rand, String[] xs) {
        return xs[rand.nextInt(xs.length)];
    }
}
