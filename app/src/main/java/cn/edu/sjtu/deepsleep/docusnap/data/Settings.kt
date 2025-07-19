package cn.edu.sjtu.deepsleep.docusnap.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Global constants
object AppConstants {
    const val DEFAULT_BACKEND_URL = "https://docusnap.zjyang.dev/api/v1/"
    const val BACKEND_GITHUB_URL = "https://github.com/JI-DeepSleep/DocuSnap-Backend"
    const val DEFAULT_BACKEND_PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjtGbqgL4BcM/mWUhCI3Z
2Zd+sTrtZT5H9SLI7khSREELR4gh3mxqq1qSLcnWt99ERK7qpjihAmjY0iRuuizc
pj9YH08XolZKv5WWV40ORtgfb7ROSGf3xlmdKa+FTwJCYEa759DMyADx9ciOEIqU
MEuy/pYc8fo0SPpK9+ANEvLHXPwIW1/WZKwjcmSWhVBznUUf9yMWF2Hp3aFSrQ++
ZNaS7vqDz+6RdhYFYY7Toc/zvf9725iV0gRNqRtWo5ywWs4ySFAiq097zS1FAU1s
Ij0GKrIiO3KcSZrxeJBntuyOHyWBVrg6J9yiDTZbMj+9uImxla4qgeoxHr5OY2x7
1QIDAQAB
-----END PUBLIC KEY-----
"""
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