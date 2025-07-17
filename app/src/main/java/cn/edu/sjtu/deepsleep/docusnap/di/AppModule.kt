package cn.edu.sjtu.deepsleep.docusnap.di

import cn.edu.sjtu.deepsleep.docusnap.data.repository.DocumentRepository
import cn.edu.sjtu.deepsleep.docusnap.service.BackendApiService
import cn.edu.sjtu.deepsleep.docusnap.service.DeviceDBService
import cn.edu.sjtu.deepsleep.docusnap.ui.viewmodels.DocumentViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Services
    single { DeviceDBService(androidContext()) }
    single { BackendApiService(androidContext()) }
    
    // Repository
    single { DocumentRepository(get(), get()) }
    
    // ViewModels
    viewModel { DocumentViewModel(get()) }
} 