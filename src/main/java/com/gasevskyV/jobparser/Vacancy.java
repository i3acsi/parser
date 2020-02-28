package com.gasevskyV.jobparser;

import com.google.common.base.Joiner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public class Vacancy {
    private int id;
    private String title;
    private String description;
    private String url;
    private String author;
    private String authorURL;
    private LocalDateTime dateCreation;

    public Vacancy(int id, String title, String description, String url, String author, String authorURL, LocalDateTime dateCreation) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.author = author;
        this.authorURL = authorURL;
        this.dateCreation = dateCreation;
    }

    public Vacancy(String title, String description, String url, String author, String authorURL, LocalDateTime dateCreation) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.author = author;
        this.authorURL = authorURL;
        this.dateCreation = dateCreation;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthorURL() {
        return authorURL;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    private String getPureTitle(String title) {
        return title.substring(0, title.indexOf("["));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Vacancy vacancy = (Vacancy) o;
        String t = getPureTitle(this.title);
        String t2 = getPureTitle(vacancy.title);
        return Objects.equals(t, t2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPureTitle(title));
    }

    @Override
    public String toString() {
        return Joiner.on(System.lineSeparator()).join("Vacancy{",
                String.format("id= %d", id),
                String.format("title= %s", title),
                String.format("description= %s", description),
                String.format("url= %s", url),
                String.format("author= %s", author),
                String.format("dateCreation= %s", dateCreation.toString()));
    }

    public static Vacancy getVac(ResultSet input) throws SQLException {
        String title = input.getString("title");
        String description = input.getString("description");
        String url = input.getString("url");
        String author = input.getString("author");
        String authorURL = input.getString("author_URL");
        LocalDateTime dateCreation = input.getTimestamp("date_creation").toLocalDateTime();
        return new Vacancy(title, description, url, author, authorURL, dateCreation);
    }
}
