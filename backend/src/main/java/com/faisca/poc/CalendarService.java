package com.faisca.poc;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.google.api.client.http.HttpRequestInitializer;
import java.util.*;

@Service
public class CalendarService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    // Tokens em memoria (POC - em producao, salvaria no banco)
    private String accessToken;
    private String refreshToken;

    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/calendar"
    );

    // Monta a URL pra redirecionar o usuario pro Google
    public String getAuthorizationUrl() throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                SCOPES
        ).setAccessType("offline").build();

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
    }

    // Troca o codigo que o Google mandou por tokens
    public void exchangeCodeForTokens(String code) throws IOException, GeneralSecurityException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();

        this.accessToken = tokenResponse.getAccessToken();
        this.refreshToken = tokenResponse.getRefreshToken();

        System.out.println("=== CONECTADO COM SUCESSO ===");
        System.out.println("Access Token: " + accessToken.substring(0, 20) + "...");
        if (refreshToken != null) {
            System.out.println("Refresh Token: " + refreshToken.substring(0, 20) + "...");
        }
    }

    public boolean isConnected() {
        return accessToken != null;
    }

    // Cria o objeto Calendar pra fazer requisicoes
    private Calendar getCalendarService() {
        final String token = this.accessToken;
        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + token);
        };

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("Faisca Arquitetura POC").build();
    }

    // Lista os proximos eventos (e os recentes, para nao sumirem os cards)
    public List<Map<String, String>> listEvents() throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        Calendar calendar = getCalendarService();
        // Janela: ultimos 30 dias ate o futuro — eventos criados com datas recentes
        // continuam aparecendo como cards em vez de sumirem da listagem.
        DateTime timeMin = new DateTime(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);

        Events events = calendar.events().list("primary")
                .setMaxResults(50)
                .setTimeMin(timeMin)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        long todayStart = LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        // A query vem ordenada por inicio (asc). Separa em futuros (de hoje em diante)
        // e passados, para priorizar os futuros sem cortar os recentes.
        List<Map<String, String>> upcoming = new ArrayList<>();
        List<Map<String, String>> past = new ArrayList<>();
        for (Event event : events.getItems()) {
            String summary = event.getSummary() != null ? event.getSummary() : "";
            if (isIgnoredEvent(summary)) continue;
            long startMillis = event.getStart().getDateTime() != null
                    ? event.getStart().getDateTime().getValue()
                    : event.getStart().getDate().getValue();
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", event.getId());
            item.put("summary", summary);
            item.put("description", event.getDescription() != null ? event.getDescription() : "");
            item.put("start", event.getStart().getDateTime() != null
                    ? event.getStart().getDateTime().toString()
                    : event.getStart().getDate().toString());
            item.put("end", event.getEnd() != null ? (
                    event.getEnd().getDateTime() != null
                            ? event.getEnd().getDateTime().toString()
                            : event.getEnd().getDate().toString()) : "");
            (startMillis >= todayStart ? upcoming : past).add(item);
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> item : upcoming) {
            if (result.size() >= 10) break;
            result.add(item);
        }
        Collections.reverse(past); // mais recentes primeiro
        for (Map<String, String> item : past) {
            if (result.size() >= 10) break;
            result.add(item);
        }
        return result;
    }

    // Ignora eventos de aniversario ("Parabens!") que o Google cria como recorrentes
    private boolean isIgnoredEvent(String summary) {
        String normalized = java.text.Normalizer
                .normalize(summary.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.contains("parabens");
    }

    // Cria um evento de teste
    public String createTestEvent() throws IOException, GeneralSecurityException {
        return createEvent("[POC] Revisao de Projeto - Faisca Arquitetura",
                "Evento de teste criado pela POC.",
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(2).toString());
    }

    // Cria evento com dados customizados
    public String createEvent(String summary, String description, String startDate, String endDate) throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        endDate = normalizeEndDate(startDate, endDate);

        Calendar calendar = getCalendarService();

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description != null ? description : "")
                .setStart(new EventDateTime().setDate(new DateTime(startDate)))
                .setEnd(new EventDateTime().setDate(new DateTime(endDate)));

        Event created = calendar.events().insert("primary", event).execute();
        System.out.println("Evento criado: " + created.getHtmlLink());
        return created.getId();
    }

    // Atualiza um evento existente
    public void updateEvent(String eventId, String summary, String description, String startDate, String endDate) throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            endDate = normalizeEndDate(startDate, endDate);
        }

        Calendar calendar = getCalendarService();
        Event event = calendar.events().get("primary", eventId).execute();

        if (summary != null) event.setSummary(summary);
        if (description != null) event.setDescription(description);
        if (startDate != null && !startDate.isEmpty()) event.setStart(new EventDateTime().setDate(new DateTime(startDate)));
        if (endDate != null && !endDate.isEmpty()) event.setEnd(new EventDateTime().setDate(new DateTime(endDate)));

        calendar.events().update("primary", eventId, event).execute();
        System.out.println("Evento atualizado: " + eventId);
    }

    // Normaliza a data de fim do evento de dia inteiro: o Google trata o fim como
    // exclusivo (start + 1 dia), entao inicio == fim vira um evento de um dia real.
    // Tambem valida que a data de fim nao vem antes do inicio.
    private String normalizeEndDate(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("A data de fim deve ser igual ou posterior a data de inicio");
        }
        return end.equals(start) ? start.plusDays(1).toString() : endDate;
    }

    // Deleta um evento
    public void deleteEvent(String eventId) throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        Calendar calendar = getCalendarService();
        calendar.events().delete("primary", eventId).execute();
        System.out.println("Evento deletado: " + eventId);
    }
}
