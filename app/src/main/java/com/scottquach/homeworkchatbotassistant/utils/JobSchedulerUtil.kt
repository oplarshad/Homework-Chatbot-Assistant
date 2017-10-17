package com.scottquach.homeworkchatbotassistant.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.support.annotation.RequiresApi

import com.scottquach.homeworkchatbotassistant.Constants
import com.scottquach.homeworkchatbotassistant.jobs.JobNotifyClassEnd

import timber.log.Timber

/**
 * Created by scott on 10/11/2017.
 * Contains all helper methods that are used to manage jobs from
 * JobScheduler
 */

object JobSchedulerUtil {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun scheduleClassManagerJob(context: Context, userClass: String,
                                minimumLatency: Long, overrideDelay: Long, specificTime: Long) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val bundle = PersistableBundle()
        bundle.putString(Constants.CLASS_NAME, userClass)
        bundle.putLong(Constants.CLASS_END_TIME, specificTime)

        jobScheduler.schedule(JobInfo.Builder(Constants.JOB_CLASS_MANAGER,
                ComponentName(context, JobNotifyClassEnd::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setMinimumLatency(minimumLatency)
                .setOverrideDeadline(overrideDelay)
                .setExtras(bundle)
                .build())
        Timber.d("Job scheduled")
        Timber.d("minlatency was $minimumLatency override delay was $overrideDelay")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun scheduleAssignmentManagerJob(context: Context, userAssignment: String) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val bundle = PersistableBundle()
        bundle.putString(Constants.USER_ASSIGNMENT, userAssignment)
    }

    /**
     * Cancels all jobs that originated from this app package
     * @param context
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun cancelAllJobs(context: Context) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancelAll()
    }


    /**
     * Cancels a specific job by job id
     * @param context
     * @param jobId
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun cancelJob(context: Context, jobId: Int) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(jobId)
    }
}
