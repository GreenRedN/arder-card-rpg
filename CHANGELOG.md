# Changelog

## v2.1.0 (2026-02-06) - 직업 제안/환생 규칙 정합화
- 시작 생성 규칙 정합화: race/name/nameless만 입력, 시작은 평민+무직 고정
- 기관 방문(site) 기반 YES/NO 직업 제안(길드/기사단/마탑/교단)
- 성직자(priest) → 성기사(paladin) 루트가 실제로 발생하도록 이벤트 픽커 보강
- 시스템 카드(SYS-*)는 밸런스 테이블(effects.json)로 수치 변동하지 않도록 처리(분기 전용)
- 환생(rebirth) 엔드포인트 가드: 엔딩(사이클 종료) 이후에만 허용

## v2.1.0 (2026-02-06) - 정합성 반영판
- Root Maven parent artifactId 정합화: fantasy-sim-parent
- EventCard에 id 필드 추가(문서/코드 스키마 동기화)
- CLI 문자열 리터럴 줄바꿈 컴파일 오류 수정
- JUnit5 스모크 테스트 추가(core)

> 기능 로직/밸런스는 변경하지 않고, 빌드/문서/스키마 정합성만 개선했습니다.
