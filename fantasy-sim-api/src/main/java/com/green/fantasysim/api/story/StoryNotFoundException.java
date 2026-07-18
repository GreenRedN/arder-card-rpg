package com.green.fantasysim.api.story;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(String id) {
        super("이야기를 찾을 수 없습니다: " + id);
    }
}
