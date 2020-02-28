package com.gasevskyV.jobparser;

import org.quartz.*;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class ScheduleTask {
    private Config properties;

    public ScheduleTask() {
    }

    public void task() throws SchedulerException {
        this.properties = new Config();
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        JobDetail jobDetail = newJob(VacancySQL.class).withIdentity("job1").build();
        JobDataMap dm = jobDetail.getJobDataMap();
        dm.put("prop", properties);
        String cron = properties.get("cron.time");
        Trigger trigger = newTrigger().withIdentity("trigger1").withSchedule(cronSchedule(cron))
                .forJob("job1").build();
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
    }

    public static void main(String[] args) {
        ScheduleTask scheduleTask = new ScheduleTask();
        try {
            scheduleTask.task();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
