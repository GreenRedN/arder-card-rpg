package com.green.fantasysim.ai;

import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.engine.EventContext;

import java.util.*;

/**
 * Per (turnType, region, threat, theme) batch caching.
 * This keeps the "10 cards pre-generated" behavior without exploding cache keys.
 */
public class EventCache {
    private final int batchSize;
    private final Map<String, Deque<EventCard>> cache = new HashMap<>();

    public EventCache(int batchSize) { this.batchSize = Math.max(1, batchSize); }

    public EventCard nextOrCreate(EventContext ctx, AiClient ai) {
        String key = ctx.turnType.name()
                + "|" + ctx.tags.get("region")
                + "|" + ctx.tags.get("threat")
                + "|" + ctx.tags.get("theme");

        Deque<EventCard> q = cache.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (q.isEmpty()) {
            for (int i = 0; i < batchSize; i++) {
                String json = ai.generateEventCardJson(ctx);
                try {
                    q.add(AiParser.parseEventCardJson(json, ctx.tags));
                } catch (Exception ex) {
                    // fallback: build a minimal deterministic card
                    q.add(TemplateEventFactory.fallback(ctx, i));
                }
            }
        }
        return q.removeFirst();
    }
}
