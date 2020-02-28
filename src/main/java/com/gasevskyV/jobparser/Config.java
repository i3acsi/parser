package com.gasevskyV.jobparser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Config {
    private final Properties values = new Properties();
    private DateTimeFormatter formatter;

    public Config() {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("parser.properties")) {
            values.load(in);
            Class.forName(values.getProperty("driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    }

    public String get(String key) {
        return this.values.getProperty(key);
    }

    public void updateLastVisit() {
        Path p = Paths.get("./src/main/resources/parser.properties");
        List<String> strings = null;
        try {
            strings = Files.lines(p).map(x -> {
                String result = x;
                if (x.startsWith("last")) {
                    result = String.format("last_visit=%s", LocalDateTime.now().format(formatter));
                }
                return result;
            }).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(p))) {
            strings.forEach(writer::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
