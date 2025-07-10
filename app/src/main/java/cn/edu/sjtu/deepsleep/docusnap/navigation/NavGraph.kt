package cn.edu.sjtu.deepsleep.docusnap.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object CameraCapture : Screen("camera_capture")
    object LocalMedia : Screen("local_media")
    object ImageProcessing : Screen("image_processing")

    object DocumentGallery : Screen("document_gallery")
    object DocumentDetail : Screen("document_detail")

    object FormGallery : Screen("form_gallery")
    object FormDetail : Screen("form_detail")
}