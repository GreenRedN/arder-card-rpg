package com.green.fantasysim.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FaithValidationTest {

    @Test
    void createPlayer_acceptsKnownFaiths() {
        for (String faith : TagOntology.FAITHS) {
            assertDoesNotThrow(() -> SessionFactory.createPlayer(
                    "human", "adventurer", "commoner", "", faith
            ));
        }

        // null/blank should default to none
        assertDoesNotThrow(() -> SessionFactory.createPlayer(
                "human", "adventurer", "commoner", "", null
        ));
        assertDoesNotThrow(() -> SessionFactory.createPlayer(
                "human", "adventurer", "commoner", "", "  "
        ));
    }

    @Test
    void createPlayer_rejectsInvalidFaith() {
        assertThrows(IllegalArgumentException.class, () -> SessionFactory.createPlayer(
                "human", "adventurer", "commoner", "", "sun_cult"
        ));
        assertThrows(IllegalArgumentException.class, () -> SessionFactory.createPlayer(
                "human", "adventurer", "commoner", "", "demon_worship"
        ));
    }
}
