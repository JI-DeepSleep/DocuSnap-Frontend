package cn.edu.sjtu.deepsleep.docusnap.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ApiService(private val context: Context) {
    private val settingsManager = SettingsManager(context)
    
    // Get the current backend URL from settings
    suspend fun getBackendUrl(): String {
        return settingsManager.settings.map { it.backendUrl }.first()
    }
    
    // Example API methods that use the backend URL
    suspend fun parseDocument(imageUris: List<String>): Map<String, String> {
        val backendUrl = getBackendUrl()
        // TODO: Implement actual API call to $backendUrl/parse/document
        // For now, return mock data
        return mapOf(
            "Vendor" to "Example Vendor",
            "Date" to "2024-01-15",
            "Amount" to "$25.00"
        )
    }
    
    suspend fun parseForm(imageUris: List<String>): Pair<Map<String, String>, List<FormField>> {
        val backendUrl = getBackendUrl()
        // TODO: Implement actual API call to $backendUrl/parse/form
        // For now, return mock data
        val extractedInfo = mapOf(
            "Form Type" to "Expense Report",
            "Date" to "2024-01-15"
        )
        val formFields = listOf(
            FormField("Name", "John Doe", true),
            FormField("Department", "Engineering", true),
            FormField("Amount", "$150.00", true)
        )
        return Pair(extractedInfo, formFields)
    }
    
    suspend fun searchDocuments(query: String): List<SearchEntity> {
        val backendUrl = getBackendUrl()
        // TODO: Implement actual API call to $backendUrl/search?q=$query
        // For now, return mock data
        return emptyList()
    }
} 