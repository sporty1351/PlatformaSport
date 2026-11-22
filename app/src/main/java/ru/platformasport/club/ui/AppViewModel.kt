package ru.platformasport.club.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.platformasport.club.AppContainer
import ru.platformasport.club.PlatformaApplication
import ru.platformasport.club.data.model.ClassEvent
import ru.platformasport.club.data.model.SessionState
import ru.platformasport.club.data.model.Tariff
import ru.platformasport.club.data.remote.CrmAccount
import java.time.LocalDate

/** Состояние длительной операции: покупка, заморозка, синхронизация. */
sealed interface OpState {
    data object Idle : OpState
    data object Loading : OpState
    data object Success : OpState
    data class Error(val message: String) : OpState
}

/**
 * Одна ViewModel на всё приложение: экраны маленькие и работают с общими
 * данными (карта, баланс, расписание), поэтому дробить состояние незачем.
 */
class AppViewModel(
    private val container: AppContainer = PlatformaApplication.container
) : ViewModel() {

    val products = container.shop.products
    fun saveProduct(product: ru.platformasport.club.data.repository.Product) {
        if (session.value.profile?.isAdmin == true) container.shop.save(product)
    }
    fun deleteProduct(id: String) {
        if (session.value.profile?.isAdmin == true) container.shop.delete(id)
    }

    val session: StateFlow<SessionState> = container.session.state
    val sessionRestored: StateFlow<Boolean> = container.session.restored
    val events: StateFlow<List<ClassEvent>> = container.schedule.events
    val bookings: StateFlow<Set<String>> = container.schedule.bookings

    private val _operation = MutableStateFlow<OpState>(OpState.Idle)
    val operation: StateFlow<OpState> = _operation.asStateFlow()

    fun resetOperation() {
        _operation.value = OpState.Idle
    }

    // ------------------------------------------------------------- аккаунт

    fun signIn(account: CrmAccount) = container.session.signIn(account)

    fun signOut() = container.session.signOut()

    fun setAdminMode(enabled: Boolean) = container.session.setAdminMode(enabled)

    fun updateProfile(firstName: String, lastName: String, email: String) =
        container.session.updateProfile(firstName, lastName, email)

    fun refreshCard() {
        viewModelScope.launch { container.session.refreshCard() }
    }

    // -------------------------------------------------------------- деньги

    fun topUp(amount: Int) {
        if (amount <= 0) return
        container.session.topUp(amount)
    }

    fun purchase(tariff: Tariff, months: Int, useBalance: Boolean) {
        _operation.value = OpState.Loading
        viewModelScope.launch {
            val result = container.session.purchase(tariff, months, useBalance)
            _operation.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Оплата не прошла") }
            )
        }
    }

    // ------------------------------------------------------------ абонемент

    fun freeze(days: Int) {
        _operation.value = OpState.Loading
        viewModelScope.launch {
            val result = container.session.freeze(days)
            _operation.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Не удалось заморозить") }
            )
        }
    }

    fun unfreeze() {
        _operation.value = OpState.Loading
        viewModelScope.launch {
            val result = container.session.unfreeze()
            _operation.value = result.fold(
                onSuccess = { OpState.Success },
                onFailure = { OpState.Error(it.message ?: "Не удалось возобновить") }
            )
        }
    }

    // ----------------------------------------------------------- расписание

    fun eventsOn(date: LocalDate): List<ClassEvent> = container.schedule.eventsOn(date)

    fun book(eventId: String) = container.schedule.book(eventId)

    fun cancelBooking(eventId: String) = container.schedule.cancelBooking(eventId)

    fun upsertEvent(event: ClassEvent) = container.schedule.upsert(event)

    fun deleteEvent(eventId: String) = container.schedule.delete(eventId)

    fun resetSchedule() = container.schedule.resetToDefault()

    fun importSchedule(csv: String, replaceRange: Boolean): Result<Int> =
        container.schedule.importCsv(csv, replaceRange)

    fun csvTemplate(): String = container.schedule.csvTemplate()
}
