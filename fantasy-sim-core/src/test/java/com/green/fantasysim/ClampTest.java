package com.green.fantasysim;

import com.green.fantasysim.util.Clamp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClampTest {
    @Test
    void between_clampsToBounds() {
        assertEquals(0, Clamp.between(-5, 0, 100));
        assertEquals(100, Clamp.between(200, 0, 100));
        assertEquals(42, Clamp.between(42, 0, 100));
    }
}
