package ru.platformasport.club.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.platformasport.club.data.local.AppPreferences
import ru.platformasport.club.data.model.MembershipStatus
import ru.platformasport.club.data.model.SessionState
import ru.platformasport.club.data.model.Tariff
import ru.platformasport.club.data.model.Transaction
import ru.platformasport.club.data.model.TxType
import ru.platformasport.club.data.remote.CrmAccount
import ru.platformasport.club.data.remote.ExternalCrmApi
import ru.platformasport.club.util.monthsWord
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Единственный источник правды о пользователе: профиль, карта, баланс, операции.
 * Изменения карты всегда проходят через внешнюю систему, локально мы только кэшируем ответ.
 */
class SessionRepository(
    private val prefs: AppPreferences,
    private val crm: ExternalCrmApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _restored = MutableStateFlow(false)
    val restored: StateFlow<Boolean> = _restored.asStateFlow()

    init {
        scope.launch {
            prefs.loadSession()?.let { _state.value = it }
            _restored.value = true
        }
    }

    fun signIn(account: CrmAccount) = update {
        SessionState(
            profile = account.profile,
            card = account.card,
            balance = account.balance,
            transactions = it.transactions.ifEmpty { demoHistory(account) }
        )
    }

    fun signOut() {
        _state.value = SessionState()
        scope.launch { prefs.clearSession() }
    }

    fun setAdminMode(enabled: Boolean) = update { s ->
        s.copy(profile = s.profile?.copy(isAdmin = enabled))
    }

    fun updateProfile(firstName: String, lastName: String, email: String) = update { s ->
        s.copy(profile = s.profile?.copy(firstName = firstName, lastName = lastName, email = email))
    }

    fun topUp(amount: Int) = update { s ->
        s.copy(
            balance = s.balance + amount,
            transactions = listOf(
                tx(TxType.TOPUP, "Пополнение баланса", amount)
            ) + s.transactions
        )
    }

    /** Покупка тарифа: списываем с баланса и просим внешнюю систему продлить карту. */
    suspend fun purchase(tariff: Tariff, months: Int, useBalance: Boolean): Result<Unit> {
        val current = _state.value
        val card = current.card ?: return Result.failure(IllegalStateException("Карта не привязана"))
        val price = tariff.priceFor(months).total

        if (useBalance && current.balance < price) {
            return Result.failure(IllegalStateException("На балансе недостаточно средств"))
        }

        val result = crm.extendMembership(card.number, tariff.id, tariff.title, months)
        return result.map { updatedCard ->
            update { s ->
                s.copy(
                    card = updatedCard,
                    balance = if (useBalance) s.balance - price else s.balance,
                    transactions = buildList {
                        if (!useBalance) add(tx(TxType.TOPUP, "Оплата банковской картой", price))
                        add(
                            tx(
                                TxType.PURCHASE,
                                "${tariff.title} · $months ${monthsWord(months)}",
                                -price
                            )
                        )
                        addAll(s.transactions)
                    }
                )
            }
        }
    }

    suspend fun freeze(days: Int): Result<Unit> {
        val card = _state.value.card ?: return Result.failure(IllegalStateException("Карта не привязана"))
        return crm.freeze(card.number, days).map { updated ->
            update { it.copy(card = updated) }
        }
    }

    suspend fun unfreeze(): Result<Unit> {
        val card = _state.value.card ?: return Result.failure(IllegalStateException("Карта не привязана"))
        return crm.unfreeze(card.number).map { updated ->
            update { it.copy(card = updated) }
        }
    }

    /** Подтянуть актуальный статус карты из внешней системы. */
    suspend fun refreshCard(): Result<Unit> {
        val card = _state.value.card ?: return Result.success(Unit)
        return crm.fetchCard(card.number).map { fresh ->
            update { it.copy(card = fresh) }
        }
    }

    private fun tx(type: TxType, title: String, amount: Int) = Transaction(
        id = UUID.randomUUID().toString(),
        type = type,
        title = title,
        amount = amount,
        date = LocalDate.now(),
        time = LocalTime.now().withSecond(0).withNano(0)
    )

    /** Пара операций в истории, чтобы демо-аккаунт не выглядел пустым. */
    private fun demoHistory(account: CrmAccount): List<Transaction> {
        if (account.card.status == MembershipStatus.NONE) return emptyList()
        val today = LocalDate.now()
        return listOf(
            Transaction(
                UUID.randomUUID().toString(), TxType.PURCHASE,
                "Персональная тренировка", -1800, today.minusDays(12), LocalTime.of(19, 40)
            ),
            Transaction(
                UUID.randomUUID().toString(), TxType.TOPUP,
                "Пополнение баланса", 3000, today.minusDays(12), LocalTime.of(19, 32)
            )
        )
    }

    private inline fun update(transform: (SessionState) -> SessionState) {
        val next = transform(_state.value)
        _state.value = next
        scope.launch { prefs.saveSession(next) }
    }
}
