# DocuSnap Project Architecture Diagram

```mermaid
flowchart TB
 subgraph subGraph1["UI Layer"]
        MainActivity["MainActivity"]
        HomeScreen["Home / Search Screen"]
        DocGallery["Document/Form Display"]
        CameraScreen["Camera Capture"]
        LocalMedia["Local Media"]
        ImageProc["Image Processing"]
        SettingsScreen["Settings Screen / PIN Verification"]
        NavController["NavController"]
        Screen["Screen Routes"]
  end
 subgraph subGraph2["ViewModel Layer"]
        DocViewModel["DocumentViewModel"]
        ImageProcViewModel["ImageProcessingViewModel"]
  end
 subgraph subGraph3["Repository Layer"]
        DocRepository["DocumentRepository"]
  end
 subgraph subGraph4["Service Layer"]
        DeviceDBService["DeviceDBService"]
        ImageProcService["ImageProcService"]
        BackendApiService["BackendApiService"]
        JobPollingService["JobPollingService"]
  end
 subgraph subGraph6["Data Layer"]
        RoomDB[("Room Database")]
        SharedPrefs[("Shared Preferences")]
  end
 subgraph subGraph7["External Services"]
        CameraX["CameraX API"]
        MediaStore["MediaStore API"]
        BackendAPI["Backend API"]
  end
 subgraph subGraph8["Data Models"]
        Document["Document Entity"]
        Form["Form Entity"]
        JobEntity["Job Entity"]
        SearchEntity["Search Entity"]
  end
    MainActivity --> NavController
    Screen --> HomeScreen & DocGallery & CameraScreen & LocalMedia & ImageProc & SettingsScreen
    HomeScreen --> DocViewModel
    DocGallery --> DocViewModel
    ImageProc --> ImageProcViewModel
    JobPollingService --> BackendApiService & DeviceDBService
    DeviceDBService --> RoomDB
    SettingsScreen --> SharedPrefs
    CameraScreen --> CameraX
    LocalMedia --> MediaStore
    BackendApiService --> BackendAPI
    RoomDB --> Document & Form & JobEntity & SearchEntity
    DocViewModel --> subGraph3
    subGraph3 --> DeviceDBService
    ImageProcViewModel --> ImageProcService
    NavController --> Screen
    HomeScreen@{ shape: rect}
    DocGallery@{ shape: proc}
     MainActivity:::uiLayer
     HomeScreen:::uiLayer
     DocGallery:::uiLayer
     CameraScreen:::uiLayer
     LocalMedia:::uiLayer
     ImageProc:::uiLayer
     SettingsScreen:::uiLayer
     NavController:::navLayer
     Screen:::navLayer
     DocViewModel:::viewModelLayer
     ImageProcViewModel:::viewModelLayer
     DocRepository:::repoLayer
     DeviceDBService:::serviceLayer
     ImageProcService:::serviceLayer
     BackendApiService:::serviceLayer
     JobPollingService:::serviceLayer
     RoomDB:::dataLayer
     SharedPrefs:::dataLayer
     CameraX:::externalLayer
     MediaStore:::externalLayer
     BackendAPI:::externalLayer
     Document:::modelLayer
     Form:::modelLayer
     JobEntity:::modelLayer
     SearchEntity:::modelLayer
    classDef uiLayer fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef navLayer fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef viewModelLayer fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef repoLayer fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef serviceLayer fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    classDef dataLayer fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    classDef externalLayer fill:#e0f2f1,stroke:#004d40,stroke-width:2px
    classDef componentLayer fill:#fafafa,stroke:#424242,stroke-width:2px
    classDef modelLayer fill:#fff8e1,stroke:#f57f17,stroke-width:2px
    classDef diLayer fill:#f9fbe7,stroke:#827717,stroke-width:2px

```

## Architecture Overview

### Layer Descriptions

**UI Layer (Jetpack Compose)**

- Contains all user interface components built with Jetpack Compose
- Implements Material Design 3 principles
- Provides declarative, reactive UI components

**Navigation Layer**

- Manages screen transitions using Jetpack Navigation Compose
- Implements type-safe routing with sealed classes
- Handles deep linking and argument passing

**ViewModel Layer**

- Manages business logic and state using MVVM pattern
- Implements reactive programming with StateFlow
- Provides clean separation between UI and data layers

**Repository Layer**

- Abstracts data access through repository pattern
- Coordinates between multiple data sources
- Provides unified data interface for ViewModels

**Service Layer**

- Handles background operations and external integrations
- Manages image processing, camera operations, and API calls
- Implements job polling for long-running operations

**Data Layer**

- Local data persistence using Room Database
- Shared preferences for application settings
- File system access for image storage

**External Services**

- CameraX for camera functionality
- MediaStore for gallery access
- Backend API for remote processing

**UI Components**

- Reusable UI components for consistent design
- Specialized components for document and form display
- Interactive components for image processing

**Data Models**

- Entity classes for database operations
- Model classes for business logic
- Search and analytics data structures

**Dependency Injection**

- AppModule for service instantiation
- Factory patterns for ViewModel creation
- Singleton management for shared services
