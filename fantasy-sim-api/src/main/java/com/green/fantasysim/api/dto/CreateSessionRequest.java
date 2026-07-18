package com.green.fantasysim.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateSessionRequest {

    @NotBlank
    @Pattern(regexp = "^(human|elf|beast|dwarf)$", message = "race must be human, elf, beast, or dwarf")
    public String race;

    // required unless nameless=true
    @Size(max = 30)
    public String name;

    // optional: start as the "Nameless" (no reincarnation)
    public boolean nameless;

    public Long seed; // optional

    @AssertTrue(message = "name required unless nameless=true")
    public boolean isNameProvidedWhenNotNameless() {
        return nameless || (name != null && !name.isBlank());
    }
}
