package cn.edu.sjtu.deepsleep.docusnap.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object CameraCapture : Screen("camera_capture")
    object LocalMedia : Screen("local_media")
    object ImageProcessing : Screen("image_processing")
    object UploadedFormSelection : Screen("uploaded_form_selection")
    object FillForm : Screen("fill_form")
    object AccessDocument : Screen("access_document")
    object DocumentTextualInfo : Screen("document_textual_info")
    object DocumentImage : Screen("document_image")
    object DocumentDetail : Screen("document_detail")
    object AccessForm : Screen("access_form")
    object FormDetail : Screen("form_detail")
}