package com.green.fantasysim.engine;

import java.util.List;
import java.util.Set;

public final class TagOntology {
    private TagOntology(){}

    public static final List<String> REGIONS = List.of("north","central","west","east","south");

    public static final List<String> THEMES = List.of(
            "survival", // 생존
            "famine",   // 고갈
            "order",    // 질서
            "flow",     // 흐름
            "life",     // 생명
            "wealth",   // 풍요
            "end",      // 종언
            "decay"     // 정체/부패
    );

    public static final List<String> THREATS = List.of(
            "monster",
            "bandit",
            "demon",
            "war",
            "politics",
            "faith",
            "ruins"
    );

    // Region-specific world tags (context keys that AI may use). Values are kept small and stable.
    public static final List<String> TERRAINS = List.of("mountain","snowfield","forest","plains","desert","highland");
    public static final List<String> NATIONS  = List.of("none","empire","desert_kingdom","south_union","south_major","elf_kingdom");
    public static final List<String> FACTIONS = List.of("neutral","empire","church","demon_worshippers","beast_anti","beast_pro","dwarf_guild","elf_guard");
    public static final List<String> FAITH_SIDES = List.of("none","church","demon_worshippers");
    public static final List<String> POLITICAL_AXES = List.of(
            "none",
            "empire_desert_tension",
            "empire_union_intrigue",
            "empire_south_neutral",
            "desert_union_proxy",
            "south_union_border"
    );



    // Lore anchors (world bible): keep named cosmology visible in play.
    // overgod=sun(주신), minor gods=life/wealth/order/flow (4대 신)
    public static final List<String> DEITIES = List.of("none","sun","life","wealth","order","flow");
    // demonKing=moon_king(마왕), archdemons=end/famine/distortion/stagnation (4 고위마족)
    public static final List<String> ARCHDEMONS = List.of("none","moon_king","end","famine","distortion","stagnation");
    public static final List<String> DEMON_TIERS = List.of("none","low","mid","sin","high","king");
    public static final List<String> SINS = List.of("none","pride","greed","lust","envy","gluttony","wrath","sloth");
    public static final List<String> SITES = List.of(
            "none",
            "ma_gyeong",
            "cathedral",
            "ruins_gate",
            // job-route anchors
            "adventurer_guild",
            "knight_order",
            "mage_tower"
    );

    // Choices:
    // - narrative alignment choices (default)
    // - binary prompts used by system events (e.g. job offers)
    public static final Set<String> ALIGN_CHOICES = Set.of("GOOD","NEUTRAL","EVIL");
    public static final Set<String> BINARY_CHOICES = Set.of("YES","NO");
    public static final Set<String> JOBS = Set.of("none","adventurer","knight","mage","priest","paladin");
    public static final Set<String> RACES = Set.of("human","elf","beast","dwarf");
    public static final Set<String> ORIGINS = Set.of("commoner","noble","royal","orphan_cult","nameless");

    // Player faith (used to bias faith/demon events). Keep it small and world-bible-aligned.
    public static final Set<String> FAITHS = Set.of("none","church","demon_worshippers");

    public static boolean isValidChoice(String id) { return ALIGN_CHOICES.contains(id) || BINARY_CHOICES.contains(id); }
}
