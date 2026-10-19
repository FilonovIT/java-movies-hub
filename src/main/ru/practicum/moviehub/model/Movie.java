package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private int id;
    private String title;
    private Integer year;

    public Movie() {
    }

    public Movie(int id, String title, Integer year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id
                && Objects.equals(title, movie.title)
                && Objects.equals(year, movie.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, year);
    }

    @Override
    public String toString() {
        return "Movie{id=" + id + ", title='" + title + "', year=" + year + "}";
    }
}