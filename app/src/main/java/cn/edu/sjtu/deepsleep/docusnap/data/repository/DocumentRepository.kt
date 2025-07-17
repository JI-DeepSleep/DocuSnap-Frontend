
import cn.edu.sjtu.deepsleep.docusnap.data.local.DocumentDao
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ProcessRequest
import cn.edu.sjtu.deepsleep.docusnap.data.remote.ProcessResponse
import cn.edu.sjtu.deepsleep.docusnap.util.CryptoUtil
import cn.edu.sjtu.deepsleep.docusnap.util.JsonUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val apiService: ApiService,
    // You may inject these utils if using DI
    private val jsonUtil: JsonUtil,         // JSON (de)serialization
    private val cryptoUtil: CryptoUtil      // Encryption/AES/SHA256
) {
    fun getAllDocuments(): Flow<List<Document>> =
        documentDao.getAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getDocument(id: String): Document? =
        documentDao.getById(id)?.toModel()

    suspend fun addDocument(doc: Document) {
        documentDao.insert(doc.toEntity())
    }

    suspend fun processDocument(document: Document): Document {
        // (1) Serialize Document to JSON
        val documentJson = jsonUtil.toJson(document, Document::class.java)
        // (2) Encrypt and encode as per backend contract
        val (encryptedContent, aesKey) = cryptoUtil.encryptAndEncode(documentJson)
        val sha256 = cryptoUtil.sha256(documentJson)

        // (3) Build ProcessRequest
        val request = ProcessRequest(
            clientId = "your-client-id", // supply actual client id
            type = "doc",
            sha256 = sha256,
            hasContent = true,
            content = encryptedContent,
            aesKey = aesKey
        )

        // (4) Call backend (mocked or real)
        val response: ProcessResponse = apiService.processDocument(request)

        // (5) Handle backend response
        if (response.status == "completed" && response.result != null) {
            // Decrypt and parse result JSON to Document
            val resultJson = cryptoUtil.decryptAndDecode(response.result, aesKey)
            val processedDoc = jsonUtil.fromJson(resultJson, Document::class.java)
            if (processedDoc != null) {
                documentDao.insert(processedDoc.copy(isProcessed = true).toEntity())
                return processedDoc.copy(isProcessed = true)
            } else {
                throw Exception("Failed to parse processed document from backend result")
            }
        } else {
            // Optionally handle "processing" or "error" status
            throw Exception("Processing not completed or failed: ${response.errorDetail}")
        }
    }
}