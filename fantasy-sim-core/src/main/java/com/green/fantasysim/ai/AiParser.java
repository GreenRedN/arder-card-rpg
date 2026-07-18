package com.green.fantasysim.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.domain.Choice;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.engine.TagOntology;

import java.util.*;

public final class AiParser {
    private static final ObjectMapper om = new ObjectMapper();
    private AiParser(){}

    public static EventCard parseEventCardJson(String json, TagSet ctxTags) {
        try {
            JsonNode n = om.readTree(json);
            EventCard c = new EventCard();
            c.id = text(n, "id", "E-unknown");
            c.title = text(n, "title", "(untitled)");
            c.situation = text(n, "situation", "");

            // tags
            c.tags = new ArrayList<>();
            JsonNode tags = n.get("tags");
            if (tags != null && tags.isArray()) {
                for (JsonNode t : tags) {
                    String s = t.asText();
                    if (!s.isBlank()) c.tags.add(s);
                }
            } else {
                // minimal safe tags
                c.tags.add("region:" + ctxTags.get("region"));
                c.tags.add("threat:" + ctxTags.get("threat"));
                c.tags.add("theme:" + ctxTags.get("theme"));
            }

            // choices
            c.choices = new ArrayList<>();
            JsonNode arr = n.get("choices");
            if (arr == null || !arr.isArray()) throw new IllegalArgumentException("choices missing");
            for (JsonNode x : arr) {
                Choice ch = new Choice();
                ch.id = text(x, "id", "");
                ch.text = text(x, "text", "");
                c.choices.add(ch);
            }

            validateCard(c, ctxTags);
            return c;
        } catch (Exception e) {
            throw new RuntimeException("AI JSON parse/validate failed", e);
        }
    }

    public static void validateCard(EventCard c, TagSet ctxTags) {
        if (c == null) throw new IllegalArgumentException("card null");
        if (c.title == null || c.title.isBlank()) throw new IllegalArgumentException("title empty");
        if (c.situation == null || c.situation.isBlank()) throw new IllegalArgumentException("situation empty");
        if (c.choices == null || (c.choices.size() != 3 && c.choices.size() != 2))
            throw new IllegalArgumentException("choices must be length 3 (GOOD/NEUTRAL/EVIL) or length 2 (YES/NO)");

        Set<String> seen = new HashSet<>();
        for (Choice ch : c.choices) {
            if (ch.id == null || ch.id.isBlank()) throw new IllegalArgumentException("choice id empty");
            if (!TagOntology.isValidChoice(ch.id)) throw new IllegalArgumentException("invalid choice id: " + ch.id);
            if (ch.text == null || ch.text.isBlank()) throw new IllegalArgumentException("choice text empty");
            seen.add(ch.id);
        }
        if (c.choices.size() == 3) {
            if (!seen.containsAll(TagOntology.ALIGN_CHOICES))
                throw new IllegalArgumentException("choices must include GOOD/NEUTRAL/EVIL exactly");
        } else {
            if (!seen.containsAll(TagOntology.BINARY_CHOICES))
                throw new IllegalArgumentException("choices must include YES/NO exactly");
        }

        // tag subset enforcement: only allow key:value where value equals ctxTags
        if (c.tags == null) c.tags = new ArrayList<>();
        for (String s : c.tags) {
            if (!s.contains(":")) throw new IllegalArgumentException("tag must be key:value, got " + s);
            String[] kv = s.split(":", 2);
            String k = kv[0];
            String v = kv[1];
            String ctx = ctxTags.get(k);
            if (ctx == null) {
                // allow some meta keys (playerJob etc) but must still match
                ctx = ctxTags.get(k);
            }
            if (ctx == null) throw new IllegalArgumentException("tag key not in context: " + k);
            if (!Objects.equals(ctx, v)) throw new IllegalArgumentException("tag value mismatch: " + s + " (ctx=" + ctx + ")");
        }
    }

    private static String text(JsonNode n, String k, String def) {
        JsonNode v = n.get(k);
        if (v == null) return def;
        String s = v.asText();
        return (s == null || s.isBlank()) ? def : s;
    }
}
