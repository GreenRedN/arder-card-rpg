package com.green.fantasysim.api.story.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateStoryCampaignRequest {
    @NotBlank
    @Size(max = 30)
    public String playerName;

    @NotBlank
    @Pattern(regexp = "^(human|elf|beast|dwarf)$", message = "race must be human, elf, beast, or dwarf")
    public String race;

    public Long seed;
}
