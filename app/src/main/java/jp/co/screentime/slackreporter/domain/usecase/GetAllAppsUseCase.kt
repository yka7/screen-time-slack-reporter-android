package jp.co.screentime.slackreporter.domain.usecase

import jp.co.screentime.slackreporter.data.repository.AppListRepository
import jp.co.screentime.slackreporter.domain.model.App
import javax.inject.Inject

class GetAllAppsUseCase @Inject constructor(
    private val appListRepository: AppListRepository
) {
    suspend operator fun invoke(): List<App> {
        return appListRepository.getAllApps()
    }
}
