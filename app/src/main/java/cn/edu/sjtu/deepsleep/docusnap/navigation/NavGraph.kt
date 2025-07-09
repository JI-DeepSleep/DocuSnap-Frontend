package cn.edu.sjtu.deepsleep.docusnap.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object CameraCapture : Screen("camera_capture")
    object LocalMedia : Screen("local_media")
    object ImageProcessing : Screen("image_processing")

    object DocumentOverview : Screen("document_overview")
    object DocumentTextualInfo : Screen("document_textual_info")
    object DocumentGallery : Screen("document_gallery")
    object DocumentDetail : Screen("document_detail")

    object FormOverview : Screen("form_overview")
    object FormDetail : Screen("form_detail")
    object FormSelection2Fill : Screen("form_selection2fill")
    object FormAutoFill : Screen("form_autofill")
}