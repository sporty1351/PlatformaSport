@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class)

package ru.platformasport.club.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------- пользователь

@Serializable
data class UserProfile(
    val id: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val email: String = "",
    val birthDate: LocalDate? = null,
    val isAdmin: Boolean = false
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val initials: String
        get() = buildString {
            firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
            lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifEmpty { "П" }
}

// -------------------------------------------------------------- клубная карта

enum class MembershipStatus { NONE, ACTIVE, FROZEN, EXPIRED }

/**
 * Карта заводится не в приложении, а во внешней системе учёта клуба.
 * Приложение отображает сведения об абонементе. Карту предъявляют на ресепшене.
 */
@Serializable
data class ClubCard(
    val number: String,
    val holder: String,
    val clubName: String = "Платформа · Ижевск",
    val tariffTitle: String,
    val issuedAt: LocalDate,
    val validUntil: LocalDate,
    val status: MembershipStatus,
    val totalDays: Int = 0,
    val freezeDaysLeft: Int = 0,
    val guestVisitsLeft: Int = 0,
    val visitsThisMonth: Int = 0
) {
    val daysLeft: Int
        get() = ChronoUnit.DAYS.between(LocalDate.now(), validUntil).toInt().coerceAtLeast(0)

    val isUsable: Boolean get() = status == MembershipStatus.ACTIVE && daysLeft > 0

    val statusTitle: String
        get() = when {
            status == MembershipStatus.FROZEN -> "Заморожен"
            status == MembershipStatus.NONE -> "Нет абонемента"
            daysLeft <= 0 -> "Истёк"
            else -> "Действует"
        }
}

// -------------------------------------------------------------------- тарифы

@Serializable
data class TariffPrice(val months: Int, val total: Int, val perMonth: Int)

@Serializable
data class Tariff(
    val id: String,
    val title: String,
    val timeWindow: String,
    val subtitle: String,
    val plus: Boolean = false,
    val badge: String? = null,
    val perks: List<String> = emptyList(),
    val prices: List<TariffPrice>
) {
    fun priceFor(months: Int): TariffPrice =
        prices.firstOrNull { it.months == months } ?: prices.first()
}

// ------------------------------------------------------------------ занятия

enum class ClassCategory(val title: String) {
    STRENGTH("Силовые"),
    CARDIO("Кардио"),
    CYCLE("Сайкл"),
    MIND("Mind & Body"),
    DANCE("Танцы"),
    WATER("Бассейн"),
    FIGHT("Единоборства"),
    KIDS("Детские")
}

@Serializable
data class ClassEvent(
    val id: String,
    val title: String,
    val category: ClassCategory,
    val coach: String,
    val room: String,
    val date: LocalDate,
    val start: LocalTime,
    val durationMin: Int,
    val capacity: Int,
    val booked: Int = 0,
    val level: String = "Любой уровень",
    val description: String = ""
) {
    val end: LocalTime get() = start.plusMinutes(durationMin.toLong())
    val freeSlots: Int get() = (capacity - booked).coerceAtLeast(0)
    val isFull: Boolean get() = freeSlots == 0
    val isPast: Boolean
        get() = date.isBefore(LocalDate.now()) ||
            (date == LocalDate.now() && end.isBefore(LocalTime.now()))
}

// ------------------------------------------------------------------ операции

enum class TxType { TOPUP, PURCHASE, REFUND }

@Serializable
data class Transaction(
    val id: String,
    val type: TxType,
    val title: String,
    val amount: Int,
    val date: LocalDate,
    val time: LocalTime
)

// ------------------------------------------------------------------- сессия

@Serializable
data class SessionState(
    val profile: UserProfile? = null,
    val card: ClubCard? = null,
    val balance: Int = 0,
    val transactions: List<Transaction> = emptyList()
) {
    val isAuthorized: Boolean get() = profile != null
}
