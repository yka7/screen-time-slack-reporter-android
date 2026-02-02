package jp.co.screentime.slackreporter.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import jp.co.screentime.slackreporter.data.repository.AppListRepository
import jp.co.screentime.slackreporter.domain.model.App
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAllAppsUseCaseTest {

    private val appListRepository: AppListRepository = mockk()
    private val useCase = GetAllAppsUseCase(appListRepository)

    @Test
    fun `invoke calls repository getAllApps`() = runTest {
        val expectedApps = listOf(
            App("com.example.app1", "App 1"),
            App("com.example.app2", "App 2")
        )
        coEvery { appListRepository.getAllApps() } returns expectedApps

        val result = useCase()

        assertEquals(expectedApps, result)
        coVerify(exactly = 1) { appListRepository.getAllApps() }
    }
}
