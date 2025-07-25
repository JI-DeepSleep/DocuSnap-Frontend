package cn.edu.sjtu.deepsleep.docusnap

import android.content.Context
import cn.edu.sjtu.deepsleep.docusnap.data.repository.DocumentRepository
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import cn.edu.sjtu.deepsleep.docusnap.service.ImageProcService

object AppModule {

    private var deviceDBService: DeviceDBService? = null
    private var documentRepository: DocumentRepository? = null
    private var imageProcService: ImageProcService? = null

    fun provideImageProcService(context: Context): ImageProcService {
        if (imageProcService == null) {
            imageProcService = ImageProcService(context)
        }
        return imageProcService!!
    }

    fun provideDeviceDBService(context: Context): DeviceDBService {
        if (deviceDBService == null) {
            deviceDBService = DeviceDBService(context)
        }
        return deviceDBService!!
    }

    fun provideDocumentRepository(context: Context): DocumentRepository {
        if (documentRepository == null) {
            val dbService = provideDeviceDBService(context)
            documentRepository = DocumentRepository(dbService)
        }
        return documentRepository!!
    }

    fun clear() {
        deviceDBService = null
        documentRepository = null
        imageProcService = null
    }
}