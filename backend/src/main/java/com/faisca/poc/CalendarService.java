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
    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        com.google.api.client.auth.oauth2.Credential credential =
                new com.google.api.client.auth.oauth2.Credential.Builder(
                        com.google.api.client.http.BearerToken.authorizationHeaderAccessMethod()
                ).setTransport(new NetHttpTransport())
                 .setJsonFactory(GsonFactory.getDefaultInstance())
                 .setClientAuthentication(
                         new com.google.api.client.http.BasicAuthentication(clientId, clientSecret))
                 .build()
                 .setAccessToken(accessToken)
                 .setRefreshToken(refreshToken);

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("Faisca Arquitetura POC").build();
    }

    // Lista os proximos 10 eventos
    public List<Map<String, String>> listEvents() throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        Calendar calendar = getCalendarService();
        DateTime now = new DateTime(System.currentTimeMillis());

        Events events = calendar.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Map<String, String>> result = new ArrayList<>();
        for (Event event : events.getItems()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("id", event.getId());
            item.put("summary", event.getSummary());
            item.put("start", event.getStart().getDateTime() != null
                    ? event.getStart().getDateTime().toString()
                    : event.getStart().getDate().toString());
            item.put("end", event.getEnd() != null ? (
                    event.getEnd().getDateTime() != null
                            ? event.getEnd().getDateTime().toString()
                            : event.getEnd().getDate().toString()) : "");
            result.add(item);
        }
        return result;
    }

    // Cria um evento de teste
    public String createTestEvent() throws IOException, GeneralSecurityException {
        if (!isConnected()) throw new RuntimeException("Nao conectado ao Google Calendar");

        Calendar calendar = getCalendarService();

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = tomorrow.plusDays(1);

        Event event = new Event()
                .setSummary("[POC] Revisao de Projeto - Faisca Arquitetura")
                .setDescription("Evento de teste criado pela POC de integracao do Google Calendar com o Faisca Arquitetura.")
                .setStart(new EventDateTime()
                        .setDate(new DateTime(tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .setEnd(new EventDateTime()
                        .setDate(new DateTime(dayAfter.format(DateTimeFormatter.ISO_LOCAL_DATE))));

        Event created = calendar.events().insert("primary", event).execute();
        System.out.println("Evento criado: " + created.getHtmlLink());
        return created.getId();
    }
}
