package cn.edu.sjtu.deepsleep.docusnap.data

import android.content.Context
import org.json.JSONObject


// Manager of all interactions with local SQLite database

class DeviceDBService(private val context: Context) {

    // SQLite variables
    
    // TODO: Document storage operations
    suspend fun saveDocument(documentId: String, data: JSONObject){
    }
    
    suspend fun getDocument(documentId: String) {
    }
    
    suspend fun updateDocument(documentId: String, updates: JSONObject){
    }

    suspend fun exportDocuments(documentIds: List<String>){
    }

    suspend fun deleteDocuments(documentIds: List<String>){
    }

    suspend fun getDocumentGallery() {
    }

    // TODO: Form data storage operations
    suspend fun saveForm(formId: String, data: JSONObject){
    }
    
    suspend fun getForm(formId: String){
    }

    suspend fun updateForm(formId: String, updates: JSONObject){
    }

    suspend fun exportForms(formIds: List<String>){
    }

    suspend fun deleteForms(formIds: List<String>){
    }

    suspend fun getFormGallery() {
    }

    // TODO: match query with related docs / forms / textual info
    suspend fun searchByQuery(query: String) {
    }

    // TODO: fetch text info by usage frequency
    suspend fun getFrequentTextInfo() {

    }
} 