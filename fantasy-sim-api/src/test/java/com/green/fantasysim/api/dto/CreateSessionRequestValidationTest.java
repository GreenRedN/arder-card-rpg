package com.green.fantasysim.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CreateSessionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void teardown() {
        if (factory != null) factory.close();
    }

    @Test
    void rejectsInvalidRace() {
        CreateSessionRequest req = new CreateSessionRequest();
        req.race = "dragon";
        req.nameless = true; // avoid name rule
        var v = validator.validate(req);
        assertTrue(v.stream().anyMatch(x -> x.getPropertyPath().toString().equals("race")));
    }

    @Test
    void requiresNameWhenNotNameless() {
        CreateSessionRequest req = new CreateSessionRequest();
        req.race = "human";
        req.nameless = false;
        req.name = "   ";
        var v = validator.validate(req);
        assertTrue(v.stream().anyMatch(x -> x.getMessage().contains("name required")));
    }

    @Test
    void allowsMissingNameWhenNameless() {
        CreateSessionRequest req = new CreateSessionRequest();
        req.race = "elf";
        req.nameless = true;
        req.name = "";
        var v = validator.validate(req);
        assertTrue(v.isEmpty(), "nameless=true should not require name");
    }
}
