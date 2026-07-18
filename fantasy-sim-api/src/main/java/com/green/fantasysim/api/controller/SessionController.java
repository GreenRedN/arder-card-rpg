package com.green.fantasysim.api.controller;

import com.green.fantasysim.api.dto.*;
import com.green.fantasysim.api.service.SessionService;
import com.green.fantasysim.engine.SessionFactory;
import com.green.fantasysim.engine.TurnOutcome;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/sessions")
    public ResponseEntity<CreateSessionResponse> create(@Valid @RequestBody CreateSessionRequest req) {
        PlayerState p = SessionFactory.createStartingPlayer(req.race, req.name, req.nameless);
        MetaState meta = SessionFactory.createMetaFromScratch(p);

        String id = sessions.createSession(p, meta, req.seed);
        // snapshot state
        var s = sessions.get(id);
        return ResponseEntity.ok(new CreateSessionResponse(id, s.world, s.player, s.meta));
    }

    @GetMapping("/sessions/{id}/state")
    public ResponseEntity<CreateSessionResponse> state(@PathVariable String id) {
        var s = sessions.get(id);
        return ResponseEntity.ok(new CreateSessionResponse(id, s.world, s.player, s.meta));
    }

    @PostMapping("/sessions/{id}/next")
    public ResponseEntity<TurnOutcome> next(@PathVariable String id) {
        return ResponseEntity.ok(sessions.next(id));
    }

    @PostMapping("/sessions/{id}/choose")
    public ResponseEntity<TurnOutcome> choose(@PathVariable String id, @Valid @RequestBody ChooseRequest req) {
        return ResponseEntity.ok(sessions.choose(id, req.choiceId));
    }

    @PostMapping("/sessions/{id}/rebirth")
    public ResponseEntity<CreateSessionResponse> rebirth(@PathVariable String id) {
        String id2 = sessions.rebirth(id);
        var s2 = sessions.get(id2);
        return ResponseEntity.ok(new CreateSessionResponse(id2, s2.world, s2.player, s2.meta));
    }
}
