package cn.edu.sjtu.deepsleep.docusnap.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Global constants
object AppConstants {
    const val DEFAULT_BACKEND_URL = "https://docusnap.zjyang.dev/api/v1/"
    const val BACKEND_GITHUB_URL = "https://github.com/docusnap/backend"
    const val DEFAULT_BACKEND_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqvRk6hI+G8RfuFC6nrxD\n" +
            "X3K7LJrTorEhkBwWfZH2rqbK0sjMDHtOleiFKmgr3rgzVbyqFXy/eTlbIvozcVge\n" +
            "brMcD9cRLXWfq/UerNpJKuZsjKHVeMop0Q1lS5AJkkZpFEQ0osGvKgJn1UTYiaS9\n" +
            "4sfHEW/AONmzWbZvQMseU15sxF26QYaNrMb9kc8BzBW6L73Quq6LZRHSqeF71JjA\n" +
            "Mw4OtvS9pxDaRbN1FzRYGLcA3iaxSEmbsloPXipBKZJntiO9zDNGI1EQOou2GUfB\n" +
            "BIeNH+P2EW+6e8khrwpafawTMyUkxpqBK0QL8/qWgc7FblVzSDfE43aJc9jnNm3A\n" +
            "rwIDAQAB\n" +
            "-----END PUBLIC KEY-----"
}

data class AppSettings(
    val pinProtectionEnabled: Boolean = false,
    val pin: String = "",
    val backendUrl: String = AppConstants.DEFAULT_BACKEND_URL,
    val backendPublicKey: String = AppConstants.DEFAULT_BACKEND_PUBLIC_KEY
)

class SettingsManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            pinProtectionEnabled = sharedPreferences.getBoolean("pin_protection_enabled", false),
            pin = sharedPreferences.getString("pin", "") ?: "",
            backendUrl = sharedPreferences.getString("backend_url", AppConstants.DEFAULT_BACKEND_URL) ?: AppConstants.DEFAULT_BACKEND_URL,
            backendPublicKey = sharedPreferences.getString("backend_public_key", AppConstants.DEFAULT_BACKEND_PUBLIC_KEY) ?: AppConstants.DEFAULT_BACKEND_PUBLIC_KEY
        )
    }
    
    suspend fun updateSettings(settings: AppSettings) {
        sharedPreferences.edit().apply {
            putBoolean("pin_protection_enabled", settings.pinProtectionEnabled)
            putString("pin", settings.pin)
            putString("backend_url", settings.backendUrl)
            putString("backend_public_key", settings.backendPublicKey)
        }.apply()
        _settings.value = settings
    }
    
    suspend fun updatePinProtection(enabled: Boolean, pin: String = "") {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(
            pinProtectionEnabled = enabled,
            pin = if (enabled) pin else ""
        )
        updateSettings(newSettings)
    }
    
    suspend fun updateBackendUrl(url: String) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(backendUrl = url)
        updateSettings(newSettings)
    }
    
    fun getCurrentSettings(): AppSettings = _settings.value
} 