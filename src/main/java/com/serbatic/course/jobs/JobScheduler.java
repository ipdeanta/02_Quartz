package com.serbatic.course.jobs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public abstract class JobScheduler {

  public static final Map<Class, Map<String, Pattern>> jobParameters =
      new HashMap<Class, Map<String, Pattern>>() {{
        put(SimpleFilePruneJob.class, SimpleFilePruneJob.getConfParameterPatternMap());
      }};
  
  private final static String className = JobScheduler.class.getSimpleName();
  private static Logger logger = Logger.getLogger(JobScheduler.class);
  
  public static void scheduleJob(Class jobClass) throws SchedulerException {
	  
	  // Obtenemos los detalles del job
	  Map<String, String> confData = loadConfigurationData(jobClass, jobParameters.get(jobClass));
	  JobBuilder jobBuilder = JobBuilder.newJob(jobClass)
	                                  	.withIdentity(jobClass.getSimpleName());
	  for (Entry<String, String> entry : confData.entrySet()) {
		  jobBuilder.usingJobData(entry.getKey(), entry.getValue());
	  }
	  JobDetail job = jobBuilder.build();
	
	  // Obtenemos el trigger del job
	  String cronExpresion = confData.get(SimpleFilePruneJob.getCronExpressionParam());
	  Trigger trigger = buildTrigger(jobClass.getSimpleName(), cronExpresion);
	
	  // Planificamos el job
	  SchedulerFactory schFactory = new StdSchedulerFactory();
	  Scheduler sch = schFactory.getScheduler();
	  sch.scheduleJob(job, trigger);
	  sch.start();
    
  }

  private static Map<String, String> loadConfigurationData(Class jobClass, Map<String, Pattern> confPatternMap) {  
    
	  String methodName = className + ".loadConfigurationData()";
	  
	  Map<String, String> confDataMap = new HashMap<String, String>();
	  // Sin validaciones exhaustivas    
	  URL res = jobClass.getClassLoader().getResource(jobClass.getSimpleName() + ".conf");
    
	  try {		  
	      Path fileConf = Paths.get(res.toURI());
	      for (String line : Files.readAllLines(fileConf)) {
	        if (line.trim().startsWith("#")) {
	          continue; // skip description headers
	        }
	        for (Entry<String, Pattern> entry : confPatternMap.entrySet()) { 
	          if (line.matches(entry.getValue().pattern())) {
	            confDataMap.put(entry.getKey(), getParameterFromLine(entry.getValue(), line));
	            break; // parameter already matched
	          }
	        }
	      }
	  } catch (URISyntaxException e) {
		  logger.error(String.format("%1$s: configuration file %2$s not found.", methodName, res), e);
	  } catch (IOException e) {
	      logger.info(String.format("%1$s: error reading configuration file.", methodName), e);
	  }
	  
	  return confDataMap;
	  
  }

  private static String getParameterFromLine(Pattern pattern, String line) {
    
	  String parameter = null;
    Matcher lineMatcher = pattern.matcher(line);
    if (lineMatcher.matches()) {
      parameter = lineMatcher.group(1);
    }
    
    return parameter;
    
  }
  
  
  private static Trigger buildTrigger(String triggerName, String cronExpression) {
    
	  CronTrigger cronTrigger =
        TriggerBuilder.newTrigger()
                      .withIdentity("crontrigger" + triggerName, "crontriggergroup" + triggerName)
                      .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                      .build();    
    
	  return cronTrigger;
	  
  }
}
