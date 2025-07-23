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
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4xDFLQDYtdJ92/6l42KH
TCfcXUYRZ20Ly6BgnBlCc4l554ZmLHLRiV/LPTN7NRQ2M7NeFg8PZfUN991WStBy
o2l15LBypP7x0imBIYRcIpvKXLbDK0TbWSuqO9G7ueoCBfq1wcyFaE72Cny52ijy
3O4CdL4T9mHAsURSZu35QitYT91lE0yvJ9iV6OyKkWOxt0+enhE2JD4iw7J5P7yU
RFzLoTyc2vaULtvTWsNLuDkhxVhghvA5FitqkLvXgFcKmNHq8gVH+81LMu8tu4J3
k283Kg0K58mydhIQ9tYtPsAfVWlfnIHqozpN/hvIqQRms28t/MpVrjH3U5HwXffC
0QIDAQAB
-----END PUBLIC KEY-----
"""
    const val DEFAULT_FREQUENT_TEXT_INFO_COUNT = 10
}

data class AppSettings(
    val pinProtectionEnabled: Boolean = false,
    val pin: String = "",
    val backendUrl: String = AppConstants.DEFAULT_BACKEND_URL,
    val backendPublicKey: String = AppConstants.DEFAULT_BACKEND_PUBLIC_KEY,
    val frequentTextInfoCount: Int = AppConstants.DEFAULT_FREQUENT_TEXT_INFO_COUNT
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
            backendPublicKey = sharedPreferences.getString("backend_public_key", AppConstants.DEFAULT_BACKEND_PUBLIC_KEY) ?: AppConstants.DEFAULT_BACKEND_PUBLIC_KEY,
            frequentTextInfoCount = sharedPreferences.getInt("frequent_text_info_count", AppConstants.DEFAULT_FREQUENT_TEXT_INFO_COUNT)
        )
    }
    
    suspend fun updateSettings(settings: AppSettings) {
        sharedPreferences.edit().apply {
            putBoolean("pin_protection_enabled", settings.pinProtectionEnabled)
            putString("pin", settings.pin)
            putString("backend_url", settings.backendUrl)
            putString("backend_public_key", settings.backendPublicKey)
            putInt("frequent_text_info_count", settings.frequentTextInfoCount)
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
    
    suspend fun updateFrequentTextInfoCount(count: Int) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(frequentTextInfoCount = count)
        updateSettings(newSettings)
    }
    
    fun getCurrentSettings(): AppSettings = _settings.value
} 