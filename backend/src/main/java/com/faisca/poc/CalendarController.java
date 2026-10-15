package com.faisca.poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:5173")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    // Passo 1: Redireciona o usuario pro Google
    @GetMapping("/connect")
    public void connect(HttpServletResponse response) throws IOException, GeneralSecurityException {
        String url = calendarService.getAuthorizationUrl();
        response.sendRedirect(url);
    }

    // Passo 2: Google volta pra ca com o codigo de autorizacao
    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException, GeneralSecurityException {
        calendarService.exchangeCodeForTokens(code);
        response.sendRedirect("http://localhost:5173?connected=true");
    }

    // Verifica se ja ta conectado
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean connected = calendarService.isConnected();
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    // Lista eventos do calendario
    @GetMapping("/events")
    public ResponseEntity<?> listEvents() {
        try {
            List<Map<String, String>> events = calendarService.listEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Cria um evento de teste (atalho rapido)
    @PostMapping("/events/test")
    public ResponseEntity<?> createTestEvent() {
        try {
            String eventId = calendarService.createTestEvent();
            return ResponseEntity.ok(Map.of("eventId", eventId, "message", "Evento de teste criado!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Cria evento com dados customizados
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, String> body) {
        try {
            String eventId = calendarService.createEvent(
                    body.get("summary"),
                    body.get("description"),
                    body.get("startDate"),
                    body.get("endDate")
            );
            return ResponseEntity.ok(Map.of("eventId", eventId, "message", "Evento criado!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Atualiza um evento existente
    @PutMapping("/events/{eventId}")
    public ResponseEntity<?> updateEvent(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        try {
            calendarService.updateEvent(
                    eventId,
                    body.get("summary"),
                    body.get("description"),
                    body.get("startDate"),
                    body.get("endDate")
            );
            return ResponseEntity.ok(Map.of("message", "Evento atualizado!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Deleta um evento
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable String eventId) {
        try {
            calendarService.deleteEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "Evento deletado!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}