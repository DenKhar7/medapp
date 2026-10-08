package iut.butinfo3.app_mobile.model

import java.io.File

data class OcrResult(
    val file: File,
    val text: String,
    val doctor: String?,
    val address: String?
)
