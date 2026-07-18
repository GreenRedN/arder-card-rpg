package com.green.fantasysim.api.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StoryChoiceRequest {
    @NotBlank
    @Size(max = 80)
    public String choiceId;

    @NotBlank
    @Size(max = 80)
    public String requestId;
}
