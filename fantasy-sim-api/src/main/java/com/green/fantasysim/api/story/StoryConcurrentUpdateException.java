package com.green.fantasysim.api.story;

public class StoryConcurrentUpdateException extends IllegalStateException {
    public StoryConcurrentUpdateException() {
        super("다른 요청이 먼저 반영되었습니다. 최신 상태를 다시 불러와 주세요.");
    }
}
