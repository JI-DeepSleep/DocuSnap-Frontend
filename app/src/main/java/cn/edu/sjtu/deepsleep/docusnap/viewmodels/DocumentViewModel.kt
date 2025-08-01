package cn.edu.sjtu.deepsleep.docusnap.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.sjtu.deepsleep.docusnap.data.model.Document
import cn.edu.sjtu.deepsleep.docusnap.data.model.Form
import cn.edu.sjtu.deepsleep.docusnap.data.model.SearchEntity
import cn.edu.sjtu.deepsleep.docusnap.data.model.TextInfo
import cn.edu.sjtu.deepsleep.docusnap.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import  android.util.Log
import cn.edu.sjtu.deepsleep.docusnap.data.model.FileType
import java.time.LocalDate

private const val TAG = "DocumentViewModel"

class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {
    
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()
    
    private val _forms = MutableStateFlow<List<Form>>(emptyList())
    val forms: StateFlow<List<Form>> = _forms.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<SearchEntity>>(emptyList())
    val searchResults: StateFlow<List<SearchEntity>> = _searchResults.asStateFlow()
    
    private val _frequentTextInfo = MutableStateFlow<List<TextInfo>>(emptyList())
    val frequentTextInfo: StateFlow<List<TextInfo>> = _frequentTextInfo.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadDocuments()
        loadForms()
        loadFrequentTextInfo()
    }
    
    fun loadDocuments() {
        Log.d("DocumentViewModel", "Loading documents...")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = repository.getAllDocuments()
                Log.d("DocumentViewModel", "Loaded ${docs.size} documents from repository")
                _documents.value = docs
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error loading documents: ${e.message}", e)
                // Handle error - could emit error state
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadForms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _forms.value = repository.getAllForms()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchByQuery(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _searchResults.value = repository.searchByQuery(query)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadFrequentTextInfo() {
        viewModelScope.launch {
            try {
                _frequentTextInfo.value = repository.getFrequentTextInfo()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun saveDocument(document: Document) {
        viewModelScope.launch {
            try {
                repository.saveDocument(document)
                loadDocuments() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateDocument(document: Document) {
        viewModelScope.launch {
            try {
                repository.updateDocument(document)
                loadDocuments() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteDocuments(documentIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.deleteDocuments(documentIds)
                loadDocuments() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun saveForm(form: Form) {
        viewModelScope.launch {
            try {
                repository.saveForm(form)
                loadForms() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateForm(form: Form) {
        viewModelScope.launch {
            try {
                repository.updateForm(form)
                loadForms() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteForms(formIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.deleteForms(formIds)
                loadForms() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun exportDocuments(documentIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.exportDocuments(documentIds)
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error exporting documents: ${e.message}", e)
                // Handle error
            }
        }
    }
    
    fun exportForms(formIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.exportForms(formIds)
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error exporting forms: ${e.message}", e)
                // Handle error
            }
        }
    }
    
    suspend fun getDocument(documentId: String): Document? {
        return repository.getDocument(documentId)
    }
    
    suspend fun getForm(formId: String): Form? {
        return repository.getForm(formId)
    }

    // Remove the full document update
    fun updateDocumentUsage(documentId: String) {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now().toString()
                repository.incrementDocumentUsage(documentId, currentDate)
                // Remove loadDocuments() call here
            } catch (e: Exception) {
                Log.e(TAG, "Error updating usage", e)
            }
        }
    }

    fun updateFormUsage(formId: String) {
        viewModelScope.launch {
            try {
                val currentDate = LocalDate.now().toString()
                repository.incrementFormUsage(formId, currentDate)
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error updating form usage: ${e.message}", e)
            }
        }
    }
    
    fun updateExtractedInfoUsage(fileId: String, fileType: FileType, key: String) {
        viewModelScope.launch {
            try {
                when (fileType) {
                    FileType.DOCUMENT -> {
                        val document = repository.getDocument(fileId)
                        document?.let { doc ->
                            val updatedExtractedInfo = doc.extractedInfo.map { item ->
                                if (item.key == key) {
                                    item.copy(
                                        usageCount = item.usageCount + 1,
                                        lastUsed = LocalDate.now().toString()
                                    )
                                } else {
                                    item
                                }
                            }
                            val updatedDoc = doc.copy(extractedInfo = updatedExtractedInfo)
                            repository.updateDocument(updatedDoc)
                            loadDocuments() // Refresh the list
                        }
                    }
                    FileType.FORM -> {
                        val form = repository.getForm(fileId)
                        form?.let { f ->
                            val updatedExtractedInfo = f.extractedInfo.map { item ->
                                if (item.key == key) {
                                    item.copy(
                                        usageCount = item.usageCount + 1,
                                        lastUsed = LocalDate.now().toString()
                                    )
                                } else {
                                    item
                                }
                            }
                            val updatedForm = f.copy(extractedInfo = updatedExtractedInfo)
                            repository.updateForm(updatedForm)
                            loadForms() // Refresh the list
                        }
                    }
                }
                loadFrequentTextInfo() // Refresh frequent text info
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error updating extracted info usage: ${e.message}", e)
            }
        }
    }

    // Development helper: Add test data
    fun addTestData() {
        Log.d("DocumentViewModel", "addTestData called")
        viewModelScope.launch {
            try {
                Log.d("DocumentViewModel", "Calling repository.addTestData()")
                repository.addTestData()
                Log.d("DocumentViewModel", "Test data added, refreshing lists")
                loadDocuments() // Refresh the list
                loadForms() // Refresh the list
                Log.d("DocumentViewModel", "Lists refreshed")
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error adding test data: ${e.message}", e)
                // Handle error
            }
        }
    }
}

// Factory for creating DocumentViewModel with dependencies
class DocumentViewModelFactory(
    private val repository: DocumentRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 