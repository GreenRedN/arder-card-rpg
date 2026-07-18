# API Quickstart (curl)

## 1) 서버 실행
```bash
./mvnw -pl fantasy-sim-api -am -DskipTests spring-boot:run
```

## 2) 세션 생성
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"race":"human","name":"그린","nameless":false,"seed":12345}'
```

응답 JSON에서 `sessionId`를 복사합니다.

## 3) next → choose 반복
```bash
curl -X POST http://localhost:8080/api/sessions/<SESSION_ID>/next
```

`needsChoice=true`라면:

```bash
curl -X POST http://localhost:8080/api/sessions/<SESSION_ID>/choose \
  -H "Content-Type: application/json" \
  -d '{"choiceId":"YES"}'
```

## 4) 상태 조회
```bash
curl http://localhost:8080/api/sessions/<SESSION_ID>/state
```

## 5) 엔딩 이후 환생
```bash
curl -X POST http://localhost:8080/api/sessions/<SESSION_ID>/rebirth
```

- `ended=true` 이후만 가능
- `nameless=true` 시작 세션은 불가(기획 규칙)
