package com.green.fantasysim.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.green.fantasysim.io.JsonUtil;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Lightweight access wrapper for world_bible_A.json.
 * - Keep data in resources for easy editing
 * - Provide safe fallbacks (return key when not found)
 */
public final class LoreBook {
    private final JsonNode root;

    private LoreBook(JsonNode root) {
        this.root = root;
    }

    public static LoreBook loadDefault() {
        try (InputStream in = LoreBook.class.getResourceAsStream("/lore/world_bible_A.json")) {
            if (in == null) throw new IllegalStateException("lore/world_bible_A.json not found in resources");
            JsonNode r = JsonUtil.mapper().readTree(in);
            return new LoreBook(r);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lore book", e);
        }
    }

    public String continent() {
        return atText("/properNouns/continent", "대륙");
    }

    public String nation(String nationKey) {
        return atText("/properNouns/nations/" + nationKey, nationKey);
    }

    public String institution(String key) {
        return atText("/properNouns/institutions/" + key, key);
    }

    public String churchName() { return institution("church"); }
    public String demonWorshippersName() { return institution("demon_worshippers"); }
    public String dwarfGuildsName() { return institution("dwarf_guilds"); }

    public String faction(String factionKey) {
        return atText("/factions/" + factionKey, factionKey);
    }

    public String site(String siteKey) {
        String s = atText("/properNouns/sites/" + siteKey, siteKey);
        return (s == null || s.isBlank()) ? siteKey : s;
    }

    public String overgod() {
        return atText("/properNouns/cosmology/overgod", "태양신");
    }

    public String demonKing() {
        return atText("/properNouns/cosmology/demonKing", "마왕");
    }

    public String minorGod(String key) {
        return atText("/properNouns/cosmology/minorGods/" + key + "/name", key);
    }

    public String archdemon(String key) {
        return atText("/properNouns/cosmology/archdemons/" + key + "/name", key);
    }

    public String sin(String key) {
        return atText("/properNouns/cosmology/sins/" + key + "/name", key);
    }

    public Relation relationBetween(String nationA, String nationB) {
        JsonNode arr = root.at("/relations");
        if (arr == null || !arr.isArray()) return null;

        for (Iterator<JsonNode> it = arr.elements(); it.hasNext();) {
            JsonNode n = it.next();
            String a = text(n, "a");
            String b = text(n, "b");
            if ((nationA.equals(a) && nationB.equals(b)) || (nationA.equals(b) && nationB.equals(a))) {
                Relation r = new Relation();
                r.a = a; r.b = b;
                r.type = text(n, "type");
                r.notes = text(n, "notes");
                return r;
            }
        }
        return null;
    }

    public String tradeHint(String nationKey) {
        JsonNode node = root.at("/trade/" + nationKey);
        if (node == null || node.isMissingNode()) return "";
        String exp = joinFirst(node.at("/exports"), 3);
        String imp = joinFirst(node.at("/imports"), 3);
        if (!exp.isBlank() && !imp.isBlank()) return "수출(" + exp + ") / 수입(" + imp + ")";
        if (!exp.isBlank()) return "수출(" + exp + ")";
        if (!imp.isBlank()) return "수입(" + imp + ")";
        return "";
    }

    public String elfEntryPolicy() {
        String humanEntry = atText("/policies/elfEntry/humanEntry", "허가 필요");
        String screening = atText("/policies/elfEntry/screening", "심사");
        return humanEntry + " / " + screening;
    }

    public String churchNorthDispatch() {
        return atText("/policies/church/northDispatch", "북부 파견 제한");
    }

    private String atText(String jsonPointer, String fallback) {
        JsonNode n = root.at(jsonPointer);
        if (n == null || n.isMissingNode() || n.isNull()) return fallback;
        String s = n.asText();
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static String text(JsonNode obj, String field) {
        if (obj == null) return null;
        JsonNode n = obj.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static String joinFirst(JsonNode arr, int max) {
        if (arr == null || !arr.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Iterator<JsonNode> it = arr.elements(); it.hasNext() && i < max; i++) {
            if (i > 0) sb.append(", ");
            sb.append(it.next().asText());
        }
        return sb.toString();
    }

    public static final class Relation {
        public String a;
        public String b;
        public String type;
        public String notes;
    }
}
