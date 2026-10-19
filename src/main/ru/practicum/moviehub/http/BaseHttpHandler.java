package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, response.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response);
        }
    }

    protected void sendError(HttpExchange ex, int status, String errorMessage) throws IOException {
        sendJson(ex, status, gson.toJson(new ErrorResponse(errorMessage)));
    }

    protected void sendValidationError(HttpExchange ex, List<String> details) throws IOException {
        sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }
}