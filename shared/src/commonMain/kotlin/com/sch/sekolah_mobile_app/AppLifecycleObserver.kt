package com.sch.sekolah_mobile_app

import com.sch.sekolah_mobile_app.data.repository.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLifecycleObserver {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun onAppForeground(userToken: String? = null) {
        scope.launch {
            // 1. Cek conditional ETag segera
            RemoteConfigManager.instance.syncWithServer(userToken)
            // 2. Sambungkan SSE listener
            RemoteConfigManager.instance.startRealtimeStream(userToken)
        }
    }

    fun onAppBackground() {
        // Putus koneksi SSE saat di background untuk menghemat baterai & OS compliance
        RemoteConfigManager.instance.stopRealtimeStream()
    }
}

