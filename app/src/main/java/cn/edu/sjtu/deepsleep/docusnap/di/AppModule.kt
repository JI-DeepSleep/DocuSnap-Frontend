package cn.edu.sjtu.deepsleep.docusnap.di

import android.content.Context
import cn.edu.sjtu.deepsleep.docusnap.data.repository.DocumentRepository
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService

object AppModule {
    
    private var deviceDBService: DeviceDBService? = null
    private var documentRepository: DocumentRepository? = null
    
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
    }
} 