package com.green.fantasysim;

import com.green.fantasysim.ai.AiParser;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.TagSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiParserTest {

    @Test
    void parse_validJson_acceptsAndKeepsChoiceIds() {
        TagSet ctx = new TagSet()
                .put("region", "north")
                .put("threat", "monster")
                .put("theme", "survival")
                .put("difficulty", "52");

        String json = "{"
                + "\"id\":\"E-001\","
                + "\"title\":\"설원에서 들려온 울음\","
                + "\"situation\":\"설원 끝자락에서 기괴한 울음소리가 들린다.\","
                + "\"choices\":["
                + "{\"id\":\"GOOD\",\"text\":\"구조를 우선한다.\"},"
                + "{\"id\":\"NEUTRAL\",\"text\":\"경계하며 우회한다.\"},"
                + "{\"id\":\"EVIL\",\"text\":\"약탈로 이익을 챙긴다.\"}"
                + "],"
                + "\"tags\":[\"region:north\",\"threat:monster\",\"theme:survival\",\"difficulty:52\"]"
                + "}";

        EventCard c = AiParser.parseEventCardJson(json, ctx);
        assertNotNull(c);
        assertEquals("E-001", c.id);
        assertEquals(3, c.choices.size());
        assertTrue(c.choices.stream().anyMatch(x -> "GOOD".equals(x.id)));
        assertTrue(c.choices.stream().anyMatch(x -> "NEUTRAL".equals(x.id)));
        assertTrue(c.choices.stream().anyMatch(x -> "EVIL".equals(x.id)));
    }

    @Test
    void parse_rejectsTagKeyOutsideContext() {
        TagSet ctx = new TagSet()
                .put("region", "north")
                .put("threat", "monster")
                .put("theme", "survival")
                .put("difficulty", "52");

        String json = "{"
                + "\"title\":\"테스트\","
                + "\"situation\":\"테스트 상황\","
                + "\"choices\":["
                + "{\"id\":\"GOOD\",\"text\":\"A\"},"
                + "{\"id\":\"NEUTRAL\",\"text\":\"B\"},"
                + "{\"id\":\"EVIL\",\"text\":\"C\"}"
                + "],"
                + "\"tags\":[\"region:north\",\"newKey:xxx\"]"
                + "}";

        assertThrows(RuntimeException.class, () -> AiParser.parseEventCardJson(json, ctx));
    }

    @Test
    void parse_validBinaryJson_acceptsYesNo() {
        TagSet ctx = new TagSet()
                .put("region", "central")
                .put("threat", "politics")
                .put("theme", "order")
                .put("difficulty", "40")
                .put("site", "adventurer_guild");

        String json = "{"
                + "\"id\":\"SYS-JOB-OFFER:adventurer:D1\","
                + "\"title\":\"길드 제안\","
                + "\"situation\":\"모험가를 하시겠습니까?\","
                + "\"choices\":["
                + "{\"id\":\"YES\",\"text\":\"예\"},"
                + "{\"id\":\"NO\",\"text\":\"아니오\"}"
                + "],"
                + "\"tags\":[\"region:central\",\"threat:politics\",\"theme:order\",\"difficulty:40\",\"site:adventurer_guild\"]"
                + "}";

        EventCard c = AiParser.parseEventCardJson(json, ctx);
        assertNotNull(c);
        assertEquals(2, c.choices.size());
        assertTrue(c.choices.stream().anyMatch(x -> "YES".equals(x.id)));
        assertTrue(c.choices.stream().anyMatch(x -> "NO".equals(x.id)));
    }

}
