package com.faisca.poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    public void connect(HttpServletResponse response) throws IOException {
        String url = calendarService.getAuthorizationUrl();
        response.sendRedirect(url);
    }

    // Passo 2: Google volta pra ca com o codigo de autorizacao
    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
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

    // Cria um evento de teste
    @PostMapping("/events/test")
    public ResponseEntity<?> createTestEvent() {
        try {
            String eventId = calendarService.createTestEvent();
            return ResponseEntity.ok(Map.of("eventId", eventId, "message", "Evento criado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
