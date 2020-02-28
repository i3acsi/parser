package com.gasevskyV.jobparser;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;


public class VacancySQL implements AutoCloseable, Job {
    private Config properties;
    private Connection connection;
    private static final Logger log = LoggerFactory.getLogger(VacancySQL.class.getName());

    public VacancySQL(Connection conn) {
        this.properties = new Config();
        this.connection = conn;
    }

    public VacancySQL() {
        this.properties = new Config();
    }

    public void startSQL() {
        Parser parser = new Parser();
        List<Vacancy> vacancies = parser.parse(this.structureCheck());
        this.save(vacancies);
    }

    //get newest vacancy from db or create table if not exists?
    private Vacancy structureCheck() {
        Vacancy result = null;
        if (connection == null) {
            getConnection();
        }
        String sql = "CREATE TABLE if NOT EXISTS vacancy ("
                + "id SERIAL PRIMARY KEY NOT NULL,"
                + "title VARCHAR (1000),"
                + "description TEXT,"
                + "url VARCHAR (1000),"
                + "author VARCHAR (300),"
                + "author_URL VARCHAR (1000),"
                + "date_creation TIMESTAMP,"
                + "CONSTRAINT unique_vacancy UNIQUE (title, description));";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.executeUpdate();
        } catch (SQLException e) {
            log.error("SQLException on CREATE TABLE", e);
        }
        try (PreparedStatement st = connection.prepareStatement("SELECT * FROM vacancy ORDER BY date_creation DESC LIMIT 1;")) {
            log.info("EXECUTE QUERY: SELECT * FROM VACANCY ORDER BY DATE_CREATION DESC LIMIT 1");
            try (ResultSet set = st.executeQuery()) {
                if (set.next()) {
                    result = Vacancy.getVac(set);
                    log.info("GET NEWEST VACANCY FROM RESULT SET: " +result.toString());
                }
            }
        } catch (SQLException e) {
            log.error("SQLException on SELECT FROM vacancy TABLE", e);
        }
        return result;
    }


    public void save(List<Vacancy> vacancies) {
//        try {
//            connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO vacancy "
                + "(title, description, url, author, author_URL, date_creation) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ON CONSTRAINT unique_vacancy DO NOTHING;")) {

            int count = 0;
            for (Vacancy v : vacancies) {
                ps.setString(1, v.getTitle());
                ps.setString(2, v.getDescription());
                ps.setString(3, v.getUrl());
                ps.setString(4, v.getAuthor());
                ps.setString(5, v.getAuthorURL());
                ps.setTimestamp(6, Timestamp.valueOf(v.getDateCreation()));
                int res = ps.executeUpdate();
                if (res > 0) {
                    count++;
                }

            }
//            ps.executeBatch();
//            connection.commit();
//            connection.setAutoCommit(true);
            log.info(String.format("Totally added to DB %d Java vacancies. On Date %s", count, LocalDateTime.now().toString()));
            //         }
        } catch (SQLException e) {
            log.error("SQLException on SAVE", e);
        }

    }

    public List<Vacancy> loadFromDB() {
        List<Vacancy> result = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement("SELECT * FROM vacancy;")) {
            try (ResultSet set = st.executeQuery()) {
                while (set.next()) {
                    Vacancy temp = Vacancy.getVac(set);
                    result.add(temp);
                }
            }
        } catch (SQLException e) {
            log.error( "SQLException on SELECT FROM vacancy TABLE", e);
        }
        return result;
    }

    private void getConnection() {
        String username = String.valueOf(this.properties.get("username"));
        String password = String.valueOf(this.properties.get("password"));
        String url = String.valueOf(this.properties.get("url"));
        try {
            if (connection != null) {
                connection.close();
            }
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            log.error("SQLException on getConnection", e);
        }
    }

    public void clean() {
        try (PreparedStatement ps = connection.prepareStatement(
                "TRUNCATE TABLE vacancy RESTART IDENTITY;")) {
            ps.executeUpdate();

        } catch (SQLException e) {
            log.error("SQLException on TRUNCATE TABLE vacancy", e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error( "SQLException on close connection", e);
            }
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("In VacancySQL - executing its JOB at "
                + LocalDateTime.now().toString() + " by " + context.getTrigger().getDescription());
        Parser parser = new Parser();
        List<Vacancy> vacancies = parser.parse(this.structureCheck());
        try (VacancySQL sql = new VacancySQL()) {
            sql.save(vacancies);
        }
    }

    public static void main(String[] args) {
        try (VacancySQL vacancySQL = new VacancySQL()){
            vacancySQL.startSQL();
        }
    }

}