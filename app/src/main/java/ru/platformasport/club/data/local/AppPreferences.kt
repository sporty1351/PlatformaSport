package ru.platformasport.club.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.platformasport.club.data.model.ClassEvent
import ru.platformasport.club.data.model.SessionState

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("platforma")

/**
 * Локальное хранилище: сессия, расписание и записи пользователя.
 * Хранится как JSON, чтобы модель можно было менять без миграций схемы.
 */
class AppPreferences(context: Context) {

    private val store = context.applicationContext.dataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun loadSession(): SessionState? = read(KEY_SESSION) {
        json.decodeFromString(SessionState.serializer(), it)
    }

    suspend fun saveSession(state: SessionState) =
        write(KEY_SESSION, json.encodeToString(SessionState.serializer(), state))

    suspend fun clearSession() = store.edit { it.remove(KEY_SESSION) }

    suspend fun loadSchedule(): List<ClassEvent>? = read(KEY_SCHEDULE) {
        json.decodeFromString(ListSerializer(ClassEvent.serializer()), it)
    }

    suspend fun saveSchedule(events: List<ClassEvent>) =
        write(KEY_SCHEDULE, json.encodeToString(ListSerializer(ClassEvent.serializer()), events))

    suspend fun loadBookings(): Set<String> =
        store.data.first()[KEY_BOOKINGS] ?: emptySet()

    suspend fun saveBookings(ids: Set<String>) {
        store.edit { it[KEY_BOOKINGS] = ids }
    }

    private suspend fun <T> read(key: Preferences.Key<String>, parse: (String) -> T): T? {
        val raw = store.data.first()[key] ?: return null
        return runCatching { parse(raw) }.getOrNull()
    }

    private suspend fun write(key: Preferences.Key<String>, value: String) {
        store.edit { it[key] = value }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session")
        val KEY_SCHEDULE = stringPreferencesKey("schedule")
        val KEY_BOOKINGS = stringSetPreferencesKey("bookings")
    }
}
