package com.green.fantasysim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ChooseRequest {
    @NotBlank
    @Pattern(regexp = "^(GOOD|NEUTRAL|EVIL|YES|NO)$", message = "choiceId must be GOOD, NEUTRAL, EVIL, YES, or NO")
    public String choiceId;
}
