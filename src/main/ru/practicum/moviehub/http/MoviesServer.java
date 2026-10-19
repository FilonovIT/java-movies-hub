package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;
    private final int port;

    public MoviesServer() {
        this(8080);
    }

    public MoviesServer(int port) {
        this.port = port;
        this.store = new MoviesStore();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler(store));
            server.setExecutor(Executors.newFixedThreadPool(4));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + port);
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }

    public int getPort() {
        return port;
    }
}