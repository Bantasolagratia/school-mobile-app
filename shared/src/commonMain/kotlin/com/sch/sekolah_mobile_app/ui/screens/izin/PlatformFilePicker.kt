package com.sch.sekolah_mobile_app.ui.screens.izin

import androidx.compose.runtime.Composable
import com.sch.sekolah_mobile_app.data.model.SelectedFile

@Composable
expect fun rememberPlatformFilePicker(
    onFileSelected: (SelectedFile) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit
