package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new LinkedHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(0);

    public synchronized List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public synchronized Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public synchronized Movie save(String title, int year) {
        int newId = idSeq.incrementAndGet();
        Movie movie = new Movie(newId, title, year);
        movies.put(newId, movie);
        return movie;
    }

    public synchronized boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public synchronized List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() != null && m.getYear() == year)
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        movies.clear();
        idSeq.set(0);
    }
}