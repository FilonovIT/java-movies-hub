package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MoviesHandler extends BaseHttpHandler {
    private static final Pattern NUM = Pattern.compile("\\d+");
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();

            if (path.equals("/movies") || path.equals("/movies/")) {
                switch (method) {
                    case "GET" -> handleGetAll(ex, query);
                    case "POST" -> handlePost(ex);
                    default -> sendError(ex, 405, "Метод не поддерживается");
                }
            } else if (path.startsWith("/movies/")) {
                String idPart = path.substring("/movies/".length());
                if (idPart.isEmpty() || idPart.contains("/")) {
                    sendError(ex, 404, "Фильм не найден");
                    return;
                }
                if (!NUM.matcher(idPart).matches()) {
                    sendError(ex, 400, "Некорректный ID");
                    return;
                }
                int id = Integer.parseInt(idPart);
                switch (method) {
                    case "GET" -> handleGetById(ex, id);
                    case "DELETE" -> handleDelete(ex, id);
                    default -> sendError(ex, 405, "Метод не поддерживается");
                }
            } else {
                sendError(ex, 404, "Ресурс не найден");
            }
        } catch (Exception e) {
            sendError(ex, 500, "Внутренняя ошибка сервера");
        } finally {
            ex.close();
        }
    }

    private void handleGetAll(HttpExchange ex, String query) throws IOException {
        Integer yearFilter = null;
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("year".equals(kv[0])) {
                    String v = kv.length > 1 ? kv[1] : "";
                    if (!NUM.matcher(v).matches()) {
                        sendError(ex, 400, "Некорректный параметр запроса — 'year'");
                        return;
                    }
                    yearFilter = Integer.parseInt(v);
                } else {
                    sendError(ex, 400, "Некорректный параметр запроса — '" + kv[0] + "'");
                    return;
                }
            }
        }
        List<Movie> result = yearFilter == null
                ? store.findAll()
                : store.findByYear(yearFilter);
        sendJson(ex, 200, gson.toJson(result));
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().startsWith("application/json")) {
            sendError(ex, 415, "Неподдерживаемый тип содержимого");
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie reqMovie;
        try {
            reqMovie = gson.fromJson(body, Movie.class);
            if (reqMovie == null) throw new IllegalArgumentException("Пустое тело");
        } catch (Exception e) {
            sendError(ex, 422, "Некорректный JSON");
            return;
        }

        List<String> details = new ArrayList<>();
        String title = reqMovie.getTitle();
        Integer year = reqMovie.getYear();

        if (title == null || title.trim().isEmpty()) {
            details.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            details.add("название не должно превышать 100 символов");
        }

        if (year == null) {
            details.add("год обязателен");
        } else {
            int maxYear = Year.now().getValue() + 1;
            if (year < 1888 || year > maxYear) {
                details.add("год должен быть между 1888 и " + maxYear);
            }
        }

        if (!details.isEmpty()) {
            sendValidationError(ex, details);
            return;
        }

        Movie saved = store.save(title, year);
        sendJson(ex, 201, gson.toJson(saved));
    }

    private void handleGetById(HttpExchange ex, int id) throws IOException {
        Optional<Movie> m = store.findById(id);
        if (m.isEmpty()) {
            sendError(ex, 404, "Фильм не найден");
        } else {
            sendJson(ex, 200, gson.toJson(m.get()));
        }
    }

    private void handleDelete(HttpExchange ex, int id) throws IOException {
        if (store.delete(id)) {
            sendNoContent(ex);
        } else {
            sendError(ex, 404, "Фильм не найден");
        }
    }
}