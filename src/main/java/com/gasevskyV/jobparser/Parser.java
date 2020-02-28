package com.gasevskyV.jobparser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;

/*
парсинг HTML с помощью jsoup

Первым делом необходимо получить экземпляр класса Document из org.jsoup.nodes.Document с указанием на источник для разбора.
Им может выступать как локальный файл, так и ссылка.
 */

public class Parser {
    private Map<String, Integer> month;
    private Config properties;
    private static final Logger log = LoggerFactory.getLogger(Parser.class);


    public Parser() {
        this.setMonth();
        this.properties = new Config();
    }

    private void setMonth() {
        this.month = new HashMap<>(16);
        this.month.put("янв", 1);
        this.month.put("фев", 2);
        this.month.put("мар", 3);
        this.month.put("апр", 4);
        this.month.put("май", 5);
        this.month.put("июн", 6);
        this.month.put("июл", 7);
        this.month.put("авг", 8);
        this.month.put("сен", 9);
        this.month.put("окт", 10);
        this.month.put("ноя", 11);
        this.month.put("дек", 12);
    }

    public List<Vacancy> parse(Vacancy last) {
        List<Vacancy> result = new ArrayList<>();
        /*
        Использую предикат для проверки текущей вакансии. Если вакансия last, поученная из базы данных равна null,
        значит БД пуста, и получить нужно все вакансии текущего года. Если last не null то получать надо все вакансии,
        пока не получим вакансию эквивалентную последней
        */
        Predicate<Vacancy> p;
        int currentYear = LocalDate.now().getYear();
        log.info("GET THIS YEAR: " + currentYear);
        // lastVisit - дата и время последнего посещения сайта - хранится в properties
        LocalDateTime lastVisit = null;
        try {
            lastVisit = LocalDateTime.parse(properties.get("last_visit"));
            log.info("LAST VISIT DATA: " + lastVisit);
        } catch (Exception e) {
            log.error("DateTimeParse exception", e);
        }
        //lastTime - дата и время сохранения последней по времени сохранения вакансси, хранящейся в БД
        LocalDateTime lastTime;
        //во-первых смотрим - имеются ли записи в БД, т.к. если БД пуста - ее надо заполнить вакансиями этого года
        if (last != null) {
            // далле смотрим - имеется ли информация о дате и времени последнео посещения сайта
            // если - да, то смотрим все вакансии с временем сохранения с текущего по(включительно) время последнего визита
            lastVisit=null;
            if (lastVisit != null) {
                LocalDateTime finalLastVisit = lastVisit;
                p = vacancy -> {
                    boolean rslt = vacancy.getDateCreation().compareTo(finalLastVisit) <= 0;
                    log.info(String.format("PREDICATE PROCESSING(lastVisit != null): vacancy date creation: %s; finalLastVisit: %s; compare: %s",
                            vacancy.getDateCreation().toString(), finalLastVisit.toString(), rslt));
                    return rslt;
                };
            } else {
                // если (по каким-то причинам) время последнего посещения сайта неизвестно, то смотрим вакансии
                // с временем сохранения не раньше времени сохранения последней вакансии
                lastTime = last.getDateCreation();
                p = vacancy -> {
                    boolean rslt = vacancy.getDateCreation().compareTo(lastTime) <= 0;
                    log.info(String.format("PREDICATE PROCESSING(lastVisit == null): vacancy date creation: %s; lastTime: %s; compare: %s",
                            vacancy.getDateCreation().toString(), lastTime.toString(), rslt));
                    return rslt;
                };
            }
        } else {
            p = vacancy ->{
                boolean rslt = vacancy.getDateCreation().getYear() != currentYear;
                log.info(String.format("PREDICATE PROCESSING(last == null): vacancy date creation: %s; rslt = %s",
                        vacancy.getDateCreation().toString(), rslt));
                return rslt;
            };
        }
        /*
        int i - для подстановки в URL, count - счетчик полученных вакансий, flag - для выхода из цикла
         */
        int i = 1;
        int globalCount = 0;
        boolean flag = true;
        do {
            String url = String.format("https://www.sql.ru/forum/job-offers/%d", i++);
           log.info("###" + url);
            Document doc = this.getDoc(url);
            Elements vacs = doc.getElementsByAttributeValue("class", "forumTable").get(0).getElementsByTag("tr");
            int count = 0;
            for (Element element : vacs) {
                String title = element.child(1).text();
                String pureTitle = this.getPureTitle(title);
                if (this.titleCheck(title)) {//pureTitle.contains("java ") && !pureTitle.contains("java script") && !pureTitle.contains("nodejs") && !title.contains("закрыт")) {
                    String tmpURL = element.child(1).select("a").attr("href");
                    Document temp = this.getDoc(tmpURL);
                    Element vacDesc = temp.getElementsByAttributeValue("class", "msgBody").get(1);
                    String desc = vacDesc.text();
                    String author = element.child(2).text();
                    String authorURL = element.child(2).select("a").attr("href");
                    LocalDateTime date = this.getDate(element.child(5).text());
                    Vacancy vac = new Vacancy(title, desc, tmpURL, author, authorURL, date);
                    log.info("VACANCY found: " + vac.toString());
                    // соответствие предикату
                    if (p.test(vac)) {
                        log.info("BREAK LOOP with vac: " + vac.toString());
                        flag = false;
                        break;
                    }
                    result.add(vac);
                    count++;
                }
            }
            log.info(String.format("Were found %d Java vacancies. On page %s", count, doc.location()));
            globalCount += count;
        } while (flag);
        log.info(String.format("Total found %d Java vacancies. On Date %s", globalCount, LocalDate.now().toString()));
        properties.updateLastVisit();
        return result;
    }

    //логика проверки title : содержит java но не script, nodejs, закрыт
    public boolean titleCheck(String title) {
        boolean result = false;
        Iterator<String> i = Arrays.stream(title.split("\\W\\s|\\s")).map(String::toLowerCase).iterator();
        while (i.hasNext()) {
            String temp = i.next();
            if ("java".equals(temp)) {
                result = true;
                if (i.hasNext() && "script".equals(i.next())) {
                    result = false;
                    break;
                }
                while (i.hasNext()) {
                    temp = i.next();
                    if ("nodejs".equals(temp) || "[закрыт".equals(temp)) {
                        result = false;
                        break;
                    }
                }
            }
        }

        return result;
    }

    private LocalDateTime getDate(String input) {
        String[] tmpDate = input.split(", ");
        LocalDate date;
        LocalTime time;

        String[] tmpTime = tmpDate[1].split(":");
        int hour = Integer.valueOf(tmpTime[0]);
        int minute = Integer.valueOf(tmpTime[1]);
        int second = 0, nanoOfSecond = 0;
        time = LocalTime.of(hour, minute, second, nanoOfSecond);

        if ("сегодня".equals(tmpDate[0])) {
            date = LocalDate.now();
        } else if ("вчера".equals(tmpDate[0])) {
            date = LocalDate.now().minusDays(1);
        } else {
            String[] day = tmpDate[0].split(" ");
            date = LocalDate.of(Integer.valueOf(String.format("20%s", day[2])),
                    month.get(day[1]),
                    Integer.valueOf(day[0]));
        }
        return LocalDateTime.of(date, time);
    }

    private String getPureTitle(String title) {
        int index = title.indexOf("[");
        return index > 0 ? title.toLowerCase().substring(0, index) : title.toLowerCase();
    }

    private Document getDoc(String url) {
        Document result = null;
        try {
            result = Jsoup.connect(url)
                    .get();
            log.info(String.format("Connected to %s on Date %s", url, LocalDate.now().toString()));
        } catch (IOException e) {
            log.error("IOException on Document init", e);
        }
        return result;
    }

    public static void main(String[] args) {
        Arrays.stream("[p][s]".split("\\]\\[")).forEach(System.out::println);
//        Parser parser = new Parser();
//        parser.titleCheck("Java Script, москва. [new]");
    }
}
