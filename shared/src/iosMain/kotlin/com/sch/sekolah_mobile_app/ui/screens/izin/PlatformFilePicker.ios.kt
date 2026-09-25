package com.sch.sekolah_mobile_app.ui.screens.izin

import androidx.compose.runtime.Composable
import com.sch.sekolah_mobile_app.data.model.SelectedFile

@Composable
actual fun rememberPlatformFilePicker(
    onFileSelected: (SelectedFile) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    return {
        // iOS platform file picker stub
    }
}
