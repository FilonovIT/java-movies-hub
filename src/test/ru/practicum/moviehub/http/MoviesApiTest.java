package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static HttpClient client;
    private static MoviesServer server;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) server.stop();
    }

    // ---------- helpers ----------
    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(BASE + path)).GET());
    }

    private HttpResponse<String> post(String body, String ct) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(BASE + "/movies"))
                .header("Content-Type", ct)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)));
    }

    private HttpResponse<String> delete(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(BASE + path)).DELETE());
    }

    private static int extractId(String json) {
        int i = json.indexOf("\"id\":");
        int start = i + 5;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Integer.parseInt(json.substring(start, end));
    }

    // ---------- GET /movies ----------
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = get("/movies");
        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_afterAdd_returnsList() throws Exception {
        post("{\"title\":\"A\",\"year\":2000}", "application/json");
        HttpResponse<String> resp = get("/movies");
        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
        assertEquals("A", movies.get(0).getTitle());
    }

    // ---------- POST /movies ----------
    @Test
    void addMovie_valid_returns201() throws Exception {
        HttpResponse<String> resp = post(
                "{\"title\":\"Inception\",\"year\":2010}", "application/json");
        assertEquals(201, resp.statusCode());
        Movie m = gson.fromJson(resp.body(), Movie.class);
        assertTrue(m.getId() > 0);
        assertEquals("Inception", m.getTitle());
        assertEquals(2010, m.getYear());
    }

    @Test
    void addMovie_emptyTitle_returns422() throws Exception {
        HttpResponse<String> resp = post(
                "{\"title\":\"\",\"year\":2000}", "application/json");
        assertEquals(422, resp.statusCode());
        ErrorResponse er = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", er.getError());
        assertTrue(er.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void addMovie_longTitle_returns422() throws Exception {
        String title = "x".repeat(101);
        HttpResponse<String> resp = post(
                "{\"title\":\"" + title + "\",\"year\":2000}", "application/json");
        assertEquals(422, resp.statusCode());
    }

    @Test
    void addMovie_badYear_returns422() throws Exception {
        HttpResponse<String> resp = post(
                "{\"title\":\"A\",\"year\":1800}", "application/json");
        assertEquals(422, resp.statusCode());
    }

    @Test
    void addMovie_wrongContentType_returns415() throws Exception {
        HttpResponse<String> resp = post(
                "{\"title\":\"A\",\"year\":2000}", "text/plain");
        assertEquals(415, resp.statusCode());
    }

    @Test
    void addMovie_brokenJson_returns422() throws Exception {
        HttpResponse<String> resp = post("{not json", "application/json");
        assertEquals(422, resp.statusCode());
    }

    // ---------- GET /movies/{id} ----------
    @Test
    void getMovieById_exists_returns200() throws Exception {
        HttpResponse<String> created = post(
                "{\"title\":\"X\",\"year\":1999}", "application/json");
        int id = extractId(created.body());

        HttpResponse<String> resp = get("/movies/" + id);
        assertEquals(200, resp.statusCode());
        Movie m = gson.fromJson(resp.body(), Movie.class);
        assertEquals(id, m.getId());
        assertEquals("X", m.getTitle());
    }

    @Test
    void getMovieById_notFound_returns404() throws Exception {
        HttpResponse<String> resp = get("/movies/999999");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovieById_notNumber_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");
        assertEquals(400, resp.statusCode());
    }

    // ---------- DELETE /movies/{id} ----------
    @Test
    void deleteMovie_exists_returns204() throws Exception {
        HttpResponse<String> created = post(
                "{\"title\":\"D\",\"year\":2020}", "application/json");
        int id = extractId(created.body());

        HttpResponse<String> resp = delete("/movies/" + id);
        assertEquals(204, resp.statusCode());
        assertEquals(404, get("/movies/" + id).statusCode());
    }

    @Test
    void deleteMovie_notFound_returns404() throws Exception {
        assertEquals(404, delete("/movies/999999").statusCode());
    }

    @Test
    void deleteMovie_notNumber_returns400() throws Exception {
        assertEquals(400, delete("/movies/abc").statusCode());
    }

    // ---------- GET /movies?year= ----------
    @Test
    void getMoviesByYear_returnsFiltered() throws Exception {
        post("{\"title\":\"A\",\"year\":2001}", "application/json");
        post("{\"title\":\"B\",\"year\":2001}", "application/json");
        post("{\"title\":\"C\",\"year\":2002}", "application/json");

        HttpResponse<String> resp = get("/movies?year=2001");
        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
    }

    @Test
    void getMoviesByYear_empty_returnsEmpty() throws Exception {
        HttpResponse<String> resp = get("/movies?year=1900");
        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesByYear_badParam_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies?year=abc");
        assertEquals(400, resp.statusCode());
    }

    // ---------- 405 ----------
    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpResponse<String> resp = send(
                HttpRequest.newBuilder(URI.create(BASE + "/movies"))
                        .method("PUT", HttpRequest.BodyPublishers.noBody()));
        assertEquals(405, resp.statusCode());
    }
}