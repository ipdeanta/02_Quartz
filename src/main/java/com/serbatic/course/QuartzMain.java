package com.serbatic.course;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

import com.serbatic.course.jobs.JobScheduler;
import com.serbatic.course.jobs.SimpleFilePruneJob;

public class QuartzMain {
  
  private final static String className = QuartzMain.class.getSimpleName();
  private static Logger logger = LogManager.getLogger(QuartzMain.class);
  
  public static void main(String[] args) {
	  
	  String methodName = className + ".main()";

	  logger.info(String.format("%1$s: >>>>>> Main execution started.", methodName));

	  try {
		  JobScheduler.scheduleJob(SimpleFilePruneJob.class);
	  } catch (SchedulerException e) {
		  logger.error(String.format("%1$s: error scheduling job %2$s.", methodName, SimpleFilePruneJob.class.getSimpleName()), e);
	  }

	  logger.info(String.format("%1$s: >>>>>> Main execution ended.", methodName));
    
  }

}
