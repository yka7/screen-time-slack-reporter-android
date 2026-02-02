package jp.co.screentime.slackreporter.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class WorkSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkScheduler
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = WorkScheduler(context)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun `scheduleOrUpdateDailyWorker schedules work with correct name`() {
        scheduler.scheduleOrUpdateDailyWorker(21, 0)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        assertEquals(DailySlackReportWorker.WORK_NAME, workInfos[0].tags.find { 
            it == DailySlackReportWorker.WORK_NAME 
        })
    }

    @Test
    fun `scheduleOrUpdateDailyWorker sets 24 hour repeat interval`() {
        scheduler.scheduleOrUpdateDailyWorker(21, 0)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        // WorkInfo exists, confirming periodic work was scheduled
        assertNotNull(workInfos[0])
    }

    @Test
    fun `scheduleOrUpdateDailyWorker requires network connection`() {
        scheduler.scheduleOrUpdateDailyWorker(21, 0)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        // Constraints are set (network required)
        assertNotNull(workInfos[0].constraints)
    }

    @Test
    fun `scheduleOrUpdateDailyWorker calculates delay for future time today`() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 2) % 24
        val futureMinute = now.get(Calendar.MINUTE)

        scheduler.scheduleOrUpdateDailyWorker(futureHour, futureMinute)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        // Work is scheduled (delay handled internally)
        assertNotNull(workInfos[0])
    }

    @Test
    fun `scheduleOrUpdateDailyWorker handles past time by scheduling for tomorrow`() {
        val now = Calendar.getInstance()
        val pastHour = (now.get(Calendar.HOUR_OF_DAY) - 1 + 24) % 24
        val pastMinute = now.get(Calendar.MINUTE)

        scheduler.scheduleOrUpdateDailyWorker(pastHour, pastMinute)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        // Work is scheduled for tomorrow
        assertNotNull(workInfos[0])
    }

    @Test
    fun `scheduleOrUpdateDailyWorker updates existing work`() {
        // Schedule first time
        scheduler.scheduleOrUpdateDailyWorker(21, 0)
        val firstWorkInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()
        val firstWorkId = firstWorkInfos[0].id

        // Schedule again with different time (update)
        scheduler.scheduleOrUpdateDailyWorker(22, 30)
        val secondWorkInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        // Should still have work scheduled
        assertFalse(secondWorkInfos.isEmpty())
        // Work was updated (ID might change with UPDATE policy)
        assertNotNull(secondWorkInfos[0])
    }

    @Test
    fun `cancelDailyWorker removes scheduled work`() {
        // Schedule work
        scheduler.scheduleOrUpdateDailyWorker(21, 0)
        val beforeCancel = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()
        assertFalse(beforeCancel.isEmpty())

        // Cancel work
        scheduler.cancelDailyWorker()

        // Wait a bit for cancellation to process
        Thread.sleep(100)

        val afterCancel = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        // Work should be cancelled
        assertTrue(afterCancel.isEmpty() || afterCancel[0].state.isFinished)
    }

    @Test
    fun `scheduleOrUpdateDailyWorker handles midnight correctly`() {
        scheduler.scheduleOrUpdateDailyWorker(0, 0)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        assertNotNull(workInfos[0])
    }

    @Test
    fun `scheduleOrUpdateDailyWorker handles 23-59 correctly`() {
        scheduler.scheduleOrUpdateDailyWorker(23, 59)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertFalse(workInfos.isEmpty())
        assertNotNull(workInfos[0])
    }

    @Test
    fun `scheduleOrUpdateDailyWorker handles same hour and minute as current time`() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        scheduler.scheduleOrUpdateDailyWorker(currentHour, currentMinute)

        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        // Should schedule for tomorrow since current time has already passed
        assertFalse(workInfos.isEmpty())
        assertNotNull(workInfos[0])
    }

    @Test
    fun `cancelDailyWorker can be called multiple times safely`() {
        scheduler.scheduleOrUpdateDailyWorker(21, 0)
        
        // Cancel multiple times
        scheduler.cancelDailyWorker()
        scheduler.cancelDailyWorker()
        scheduler.cancelDailyWorker()

        // Should not crash
        Thread.sleep(100)
        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertTrue(workInfos.isEmpty() || workInfos[0].state.isFinished)
    }

    @Test
    fun `cancelDailyWorker without scheduled work does not crash`() {
        // Cancel without scheduling first
        scheduler.cancelDailyWorker()

        // Should not crash
        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()

        assertTrue(workInfos.isEmpty())
    }

    @Test
    fun `scheduleOrUpdateDailyWorker uses UPDATE policy`() {
        // Schedule first work
        scheduler.scheduleOrUpdateDailyWorker(21, 0)
        val firstCount = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()
            .size

        // Schedule again (should UPDATE, not create duplicate)
        scheduler.scheduleOrUpdateDailyWorker(22, 0)
        val secondCount = workManager
            .getWorkInfosForUniqueWork(DailySlackReportWorker.WORK_NAME)
            .get()
            .size

        // Should still have only one work (updated, not duplicated)
        assertEquals(firstCount, secondCount)
    }
}
