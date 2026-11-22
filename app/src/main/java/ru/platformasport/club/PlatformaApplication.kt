package ru.platformasport.club

import android.app.Application
import android.content.Context
import ru.platformasport.club.data.local.AppPreferences
import ru.platformasport.club.data.remote.ExternalCrmApi
import ru.platformasport.club.data.remote.FakeCrmApi
import ru.platformasport.club.data.repository.ScheduleRepository
import ru.platformasport.club.data.repository.SessionRepository

/**
 * Ручной контейнер зависимостей. Приложение небольшое, поэтому DI-фреймворк
 * пока не нужен: всё собирается в одном месте и легко подменяется в тестах.
 */
class AppContainer(context: Context) {
    private val preferences = AppPreferences(context)

    /** Здесь меняется заглушка на реальную реализацию, когда появится система клуба. */
    val crm: ExternalCrmApi = FakeCrmApi()

    val session = SessionRepository(preferences, crm)
    val schedule = ScheduleRepository(preferences)
}

class PlatformaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        lateinit var container: AppContainer
            private set
    }
}
