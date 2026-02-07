package jp.co.screentime.slackreporter.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.domain.model.AppUsage
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GetTodayUsageUseCaseTest {

    @Test
    fun `returns usage data sorted by duration descending`() = runTest {
        val repository = mockk<UsageRepository>()
        val mockUsage = listOf(
            AppUsage("com.chrome", 1800000L),
            AppUsage("com.youtube", 3600000L),
            AppUsage("com.twitter", 900000L)
        )
        coEvery { repository.getUsage(any(), any()) } returns mockUsage

        val useCase = GetTodayUsageUseCase(repository, StandardTestDispatcher(testScheduler))
        val result = useCase()

        assertEquals(3, result.size)
        assertEquals("com.youtube", result[0].packageName)
        assertEquals("com.chrome", result[1].packageName)
        assertEquals("com.twitter", result[2].packageName)
    }

    @Test
    fun `filters out apps with zero duration`() = runTest {
        val repository = mockk<UsageRepository>()
        val mockUsage = listOf(
            AppUsage("com.youtube", 3600000L),
            AppUsage("com.system", 0L),
            AppUsage("com.chrome", 1800000L)
        )
        coEvery { repository.getUsage(any(), any()) } returns mockUsage

        val useCase = GetTodayUsageUseCase(repository, StandardTestDispatcher(testScheduler))
        val result = useCase()

        assertEquals(2, result.size)
        assertFalse(result.any { it.durationMillis == 0L })
    }

    @Test
    fun `returns empty list when no usage data`() = runTest {
        val repository = mockk<UsageRepository>()
        coEvery { repository.getUsage(any(), any()) } returns emptyList()

        val useCase = GetTodayUsageUseCase(repository, StandardTestDispatcher(testScheduler))
        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
