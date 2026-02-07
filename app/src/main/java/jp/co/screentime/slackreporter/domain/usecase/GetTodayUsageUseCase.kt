package jp.co.screentime.slackreporter.domain.usecase

import jp.co.screentime.slackreporter.data.repository.UsageRepository
import jp.co.screentime.slackreporter.di.IoDispatcher
import jp.co.screentime.slackreporter.domain.model.AppUsage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

/**
 * 今日の利用状況を取得するユースケース
 *
 * 当日0:00から現在時刻までのアプリ別利用時間を取得する
 */
class GetTodayUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * 今日の利用状況を取得
     *
     * @return アプリ別利用時間のリスト（利用時間降順）
     */
    suspend operator fun invoke(): List<AppUsage> {
        return withContext(ioDispatcher) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            usageRepository.getUsage(startOfDay, now)
                .filter { it.hasUsage }
                .sortedByDescending { it.durationMillis }
        }
    }
}
