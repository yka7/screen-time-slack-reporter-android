package jp.co.screentime.slackreporter.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import jp.co.screentime.slackreporter.domain.model.AppUsage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTodayUsedAppsUseCaseTest {

    private val getTodayUsageUseCase: GetTodayUsageUseCase = mockk()
    private val useCase = GetTodayUsedAppsUseCase(getTodayUsageUseCase)

    @Test
    fun `invoke delegates to getTodayUsageUseCase`() = runTest {
        val expectedUsage = listOf(
            AppUsage("com.example.app", 1000L)
        )
        coEvery { getTodayUsageUseCase() } returns expectedUsage

        val result = useCase()

        assertEquals(expectedUsage, result)
        coVerify(exactly = 1) { getTodayUsageUseCase() }
    }
}
