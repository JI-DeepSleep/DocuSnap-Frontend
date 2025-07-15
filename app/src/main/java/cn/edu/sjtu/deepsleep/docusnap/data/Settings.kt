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
}

data class AppSettings(
    val pinProtectionEnabled: Boolean = false,
    val pin: String = "",
    val backendUrl: String = AppConstants.DEFAULT_BACKEND_URL
)

class SettingsManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            pinProtectionEnabled = sharedPreferences.getBoolean("pin_protection_enabled", false),
            pin = sharedPreferences.getString("pin", "") ?: "",
            backendUrl = sharedPreferences.getString("backend_url", AppConstants.DEFAULT_BACKEND_URL) ?: AppConstants.DEFAULT_BACKEND_URL
        )
    }
    
    suspend fun updateSettings(settings: AppSettings) {
        sharedPreferences.edit().apply {
            putBoolean("pin_protection_enabled", settings.pinProtectionEnabled)
            putString("pin", settings.pin)
            putString("backend_url", settings.backendUrl)
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