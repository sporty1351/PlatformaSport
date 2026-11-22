package ru.platformasport.club.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.platformasport.club.PlatformaApplication
import ru.platformasport.club.data.remote.CrmAccount
import ru.platformasport.club.data.remote.ExternalCrmApi

enum class AuthStep { PHONE, CODE, PROFILE }

data class AuthUiState(
    val step: AuthStep = AuthStep.PHONE,
    val phoneDigits: String = "7",
    val code: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val agreed: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val resendIn: Int = 0
) {
    val phoneReady: Boolean get() = phoneDigits.length == 11 && agreed
    val codeReady: Boolean get() = code.length == 4
    val profileReady: Boolean get() = firstName.isNotBlank() && lastName.isNotBlank()
    val canonicalPhone: String get() = "+$phoneDigits"
}

/**
 * Вход по номеру телефона. Аккаунт и клубная карта живут во внешней системе:
 * приложение только подтверждает номер и забирает готовые данные.
 */
class AuthViewModel(
    private val crm: ExternalCrmApi = PlatformaApplication.container.crm
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _account = MutableStateFlow<CrmAccount?>(null)
    val account: StateFlow<CrmAccount?> = _account.asStateFlow()

    /** [input] — только цифры номера без кода страны (до 10 знаков). */
    fun onPhoneChanged(input: String) {
        val rest = input.filter { it.isDigit() }.take(10)
        _state.value = _state.value.copy(phoneDigits = "7$rest", error = null)
    }

    fun toggleAgreement() {
        _state.value = _state.value.copy(agreed = !_state.value.agreed)
    }

    fun onCodeChanged(input: String) {
        val code = input.filter { it.isDigit() }.take(4)
        _state.value = _state.value.copy(code = code, error = null)
        if (code.length == 4) verifyCode()
    }

    fun onFirstName(value: String) { _state.value = _state.value.copy(firstName = value, error = null) }
    fun onLastName(value: String) { _state.value = _state.value.copy(lastName = value, error = null) }
    fun onEmail(value: String) { _state.value = _state.value.copy(email = value, error = null) }

    fun back() {
        _state.value = when (_state.value.step) {
            AuthStep.PHONE -> _state.value
            AuthStep.CODE -> _state.value.copy(step = AuthStep.PHONE, code = "", error = null)
            AuthStep.PROFILE -> _state.value.copy(step = AuthStep.CODE, error = null)
        }
    }

    fun requestCode() {
        val current = _state.value
        if (!current.phoneReady || current.loading) return
        _state.value = current.copy(loading = true, error = null)
        viewModelScope.launch {
            crm.requestSmsCode(current.canonicalPhone).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        step = AuthStep.CODE,
                        loading = false,
                        code = "",
                        resendIn = RESEND_SECONDS
                    )
                    startResendTimer()
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Не получилось отправить код"
                    )
                }
            )
        }
    }

    fun verifyCode() {
        val current = _state.value
        if (!current.codeReady || current.loading) return
        _state.value = current.copy(loading = true, error = null)
        viewModelScope.launch {
            crm.verifyCode(current.canonicalPhone, current.code).fold(
                onSuccess = { found ->
                    if (found != null) {
                        _state.value = _state.value.copy(loading = false)
                        _account.value = found
                    } else {
                        _state.value = _state.value.copy(step = AuthStep.PROFILE, loading = false)
                    }
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        loading = false,
                        code = "",
                        error = it.message ?: "Код не подошёл"
                    )
                }
            )
        }
    }

    fun register() {
        val current = _state.value
        if (!current.profileReady || current.loading) return
        _state.value = current.copy(loading = true, error = null)
        viewModelScope.launch {
            crm.register(
                phone = current.canonicalPhone,
                firstName = current.firstName.trim(),
                lastName = current.lastName.trim(),
                email = current.email.trim()
            ).fold(
                onSuccess = {
                    _state.value = _state.value.copy(loading = false)
                    _account.value = it
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Не удалось создать профиль"
                    )
                }
            )
        }
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            while (_state.value.resendIn > 0) {
                delay(1000)
                _state.value = _state.value.copy(resendIn = _state.value.resendIn - 1)
            }
        }
    }

    private companion object {
        const val RESEND_SECONDS = 45
    }
}
