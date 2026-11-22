package ru.platformasport.club.data.remote

import ru.platformasport.club.data.model.ClubCard
import ru.platformasport.club.data.model.UserProfile

/**
 * Граница с внешней системой клуба (та самая, что выпускает клубные карты
 * и ведёт срок действия абонемента).
 *
 * Приложение работает только через этот интерфейс. Чтобы подключить реальную
 * систему, достаточно написать `RetrofitCrmApi : ExternalCrmApi` и подменить
 * реализацию в `AppContainer` — экраны и репозитории трогать не нужно.
 */
interface ExternalCrmApi {

    /** Отправить SMS с кодом подтверждения. */
    suspend fun requestSmsCode(phone: String): Result<Unit>

    /**
     * Проверить код. Возвращает `null`, если клиента с таким телефоном
     * во внешней системе ещё нет — тогда показываем анкету.
     */
    suspend fun verifyCode(phone: String, code: String): Result<CrmAccount?>

    /** Завести клиента и выпустить ему клубную карту. */
    suspend fun register(
        phone: String,
        firstName: String,
        lastName: String,
        email: String
    ): Result<CrmAccount>

    /** Актуальное состояние карты: статус, срок, заморозка, визиты. */
    suspend fun fetchCard(cardNumber: String): Result<ClubCard>

    /** Продлить абонемент после оплаты. */
    suspend fun extendMembership(
        cardNumber: String,
        tariffId: String,
        tariffTitle: String,
        months: Int
    ): Result<ClubCard>

    /** Поставить абонемент на паузу. */
    suspend fun freeze(cardNumber: String, days: Int): Result<ClubCard>

    /** Снять с паузы. */
    suspend fun unfreeze(cardNumber: String): Result<ClubCard>
}

/** Ответ внешней системы: профиль + карта + баланс лицевого счёта. */
data class CrmAccount(
    val profile: UserProfile,
    val card: ClubCard,
    val balance: Int
)
