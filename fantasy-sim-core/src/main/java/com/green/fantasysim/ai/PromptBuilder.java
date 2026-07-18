package com.green.fantasysim.ai;

import com.green.fantasysim.engine.EventContext;

/**
 * For real LLM integration:
 * build a strict prompt that forces JSON output + tag constraints.
 * DummyAiClient doesn't need this, but we keep it so the core is service-ready.
 */
public final class PromptBuilder {
    private PromptBuilder(){}

    public static String buildEventPrompt(EventContext ctx) {
        return "You are a game narrative generator. Output ONLY valid JSON.\n"
                + "Schema: {id,title,situation,choices:[{id,text},{id,text},{id,text}],tags:[...]}\n"
                + "Allowed choice ids: GOOD, NEUTRAL, EVIL\n"
                + "Tags must be a subset of the context keys AND each value must exactly match the context value.\n"
                + "Tag format in output: key:value (example: region:west).\n"
                + "Context tags (key=value): " + ctx.tags.signature() + "\n"
                + "World(empire,demon,cult,mood)=" + ctx.world.empire + "," + ctx.world.demon + "," + ctx.world.cult + "," + ctx.world.publicMood + "\n"
                + "Player(name,job,tier,race,origin,power)=" + safe(ctx.player.name) + "," + ctx.player.job + "," + ctx.player.tier + "," + ctx.player.race + "," + ctx.player.origin + "," + ctx.player.power + "\n";
    }

    private static String safe(String s) {
        if (s == null || s.isBlank()) return "(none)";
        return s.replace("\n", " ").replace("\r", " ").trim();
    }
}
