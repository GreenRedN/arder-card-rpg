package com.green.fantasysim.api.story;

public class StoryAccessDeniedException extends RuntimeException {
    public StoryAccessDeniedException() {
        super("이어하기 토큰이 올바르지 않습니다.");
    }
}
