package ru.platformasport.club.data.remote

import kotlinx.coroutines.delay
import ru.platformasport.club.data.model.ClubCard
import ru.platformasport.club.data.model.MembershipStatus
import ru.platformasport.club.data.model.UserProfile
import java.time.LocalDate
import kotlin.random.Random

/**
 * Заглушка внешней системы на время, пока её нет.
 * Держит данные в памяти процесса и имитирует сетевую задержку.
 *
 * Демо-аккаунты:
 *  • +7 (912) 345-67-89 — действующий абонемент «Базовая+» до +74 дней;
 *  • +7 (900) 000-00-00 — сотрудник клуба, доступен режим администратора;
 *  • любой другой номер — новый клиент: анкета, карта без абонемента.
 * Код подтверждения в демо-режиме — 1111.
 */
class FakeCrmApi : ExternalCrmApi {

    private val accounts = mutableMapOf<String, CrmAccount>()
    private val cardsByNumber = mutableMapOf<String, String>() // номер карты -> телефон

    init {
        seed(
            phone = DEMO_MEMBER_PHONE,
            first = "Артём",
            last = "Соколов",
            card = "5901240087312",
            tariff = "Базовая+",
            daysLeft = 74,
            totalDays = 180,
            balance = 1250,
            admin = false
        )
        seed(
            phone = DEMO_ADMIN_PHONE,
            first = "Ольга",
            last = "Дементьева",
            card = "5901240000001",
            tariff = "Корпоративная",
            daysLeft = 300,
            totalDays = 365,
            balance = 0,
            admin = true
        )
    }

    private fun seed(
        phone: String,
        first: String,
        last: String,
        card: String,
        tariff: String,
        daysLeft: Int,
        totalDays: Int,
        balance: Int,
        admin: Boolean
    ) {
        val today = LocalDate.now()
        val account = CrmAccount(
            profile = UserProfile(
                id = "crm-${phone.takeLast(4)}",
                phone = phone,
                firstName = first,
                lastName = last,
                email = "",
                isAdmin = admin
            ),
            card = ClubCard(
                number = card,
                holder = "$first $last",
                tariffTitle = tariff,
                issuedAt = today.minusDays((totalDays - daysLeft).toLong()),
                validUntil = today.plusDays(daysLeft.toLong()),
                status = MembershipStatus.ACTIVE,
                totalDays = totalDays,
                freezeDaysLeft = 30,
                guestVisitsLeft = 2,
                visitsThisMonth = 9
            ),
            balance = balance
        )
        accounts[phone] = account
        cardsByNumber[card] = phone
    }

    override suspend fun requestSmsCode(phone: String): Result<Unit> {
        delay(900)
        return if (phone.filter { it.isDigit() }.length == 11) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Проверьте номер телефона"))
    }

    override suspend fun verifyCode(phone: String, code: String): Result<CrmAccount?> {
        delay(700)
        if (code != DEMO_CODE) {
            return Result.failure(IllegalArgumentException("Неверный код"))
        }
        return Result.success(accounts[phone])
    }

    override suspend fun register(
        phone: String,
        firstName: String,
        lastName: String,
        email: String
    ): Result<CrmAccount> {
        delay(900)
        val today = LocalDate.now()
        val cardNumber = "59012" + Random.nextInt(10_000_000, 99_999_999).toString()
        val account = CrmAccount(
            profile = UserProfile(
                id = "crm-" + Random.nextInt(100000, 999999),
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                email = email
            ),
            card = ClubCard(
                number = cardNumber,
                holder = "$firstName $lastName",
                tariffTitle = "Абонемент не оформлен",
                issuedAt = today,
                validUntil = today,
                status = MembershipStatus.NONE,
                totalDays = 0,
                freezeDaysLeft = 0,
                guestVisitsLeft = 0
            ),
            balance = 0
        )
        accounts[phone] = account
        cardsByNumber[cardNumber] = phone
        return Result.success(account)
    }

    override suspend fun fetchCard(cardNumber: String): Result<ClubCard> {
        delay(400)
        val account = account(cardNumber) ?: return notFound()
        return Result.success(account.card)
    }

    override suspend fun extendMembership(
        cardNumber: String,
        tariffId: String,
        tariffTitle: String,
        months: Int
    ): Result<ClubCard> {
        delay(1100)
        val phone = cardsByNumber[cardNumber] ?: return notFound()
        val account = accounts.getValue(phone)
        val today = LocalDate.now()
        val from = if (account.card.validUntil.isAfter(today)) account.card.validUntil else today
        val updated = account.card.copy(
            tariffTitle = tariffTitle,
            validUntil = from.plusMonths(months.toLong()),
            issuedAt = if (account.card.status == MembershipStatus.NONE) today else account.card.issuedAt,
            status = MembershipStatus.ACTIVE,
            totalDays = account.card.totalDays + months * 30,
            freezeDaysLeft = maxOf(account.card.freezeDaysLeft, months * 5),
            guestVisitsLeft = account.card.guestVisitsLeft + if (tariffId.contains("plus")) 2 else 0
        )
        accounts[phone] = account.copy(card = updated)
        return Result.success(updated)
    }

    override suspend fun freeze(cardNumber: String, days: Int): Result<ClubCard> {
        delay(800)
        val phone = cardsByNumber[cardNumber] ?: return notFound()
        val account = accounts.getValue(phone)
        if (account.card.freezeDaysLeft < days) {
            return Result.failure(IllegalStateException("Доступно только ${account.card.freezeDaysLeft} дней заморозки"))
        }
        val updated = account.card.copy(
            status = MembershipStatus.FROZEN,
            validUntil = account.card.validUntil.plusDays(days.toLong()),
            freezeDaysLeft = account.card.freezeDaysLeft - days
        )
        accounts[phone] = account.copy(card = updated)
        return Result.success(updated)
    }

    override suspend fun unfreeze(cardNumber: String): Result<ClubCard> {
        delay(600)
        val phone = cardsByNumber[cardNumber] ?: return notFound()
        val account = accounts.getValue(phone)
        val updated = account.card.copy(status = MembershipStatus.ACTIVE)
        accounts[phone] = account.copy(card = updated)
        return Result.success(updated)
    }

    private fun account(cardNumber: String): CrmAccount? =
        cardsByNumber[cardNumber]?.let { accounts[it] }

    private fun <T> notFound(): Result<T> =
        Result.failure(NoSuchElementException("Карта не найдена во внешней системе"))

    companion object {
        const val DEMO_CODE = "1111"
        const val DEMO_MEMBER_PHONE = "+79123456789"
        const val DEMO_ADMIN_PHONE = "+79000000000"
    }
}
