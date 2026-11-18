package com.macro.app.input

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.macro.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Starts a Shizuku UserService and reports its real process identity.
 * This manager is a capability probe, not an input injector.
 */
object ShizukuProbeManager {
    private const val REQUEST_CODE = 7301
    private const val SERVICE_VERSION = 1

    private val mutableStatus = MutableStateFlow(
        "Chưa kiểm tra Shizuku. Nhấn nút kiểm tra để bắt đầu."
    )
    val status: StateFlow<String> = mutableStatus.asStateFlow()

    private var appContext: Context? = null
    private var initialized = false
    private var serviceBound = false

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindProbeService()
                } else {
                    mutableStatus.value =
                        "Bạn chưa cấp quyền Shizuku cho MacroAndroid."
                }
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val probe = IShizukuInputProbe.Stub.asInterface(binder)
            if (probe == null) {
                serviceBound = false
                mutableStatus.value = "Không nhận được Binder từ Shizuku UserService."
                return
            }

            runCatching {
                val uid = probe.processUid
                val pid = probe.processPid
                val hasInjectEvents = probe.hasInjectEventsPermission()
                val identity = when (uid) {
                    0 -> "root (UID 0)"
                    2000 -> "shell/ADB (UID 2000)"
                    else -> "UID $uid"
                }
                val permission = if (hasInjectEvents) "ĐƯỢC CẤP" else "KHÔNG ĐƯỢC CẤP"

                mutableStatus.value = buildString {
                    appendLine("Shizuku UserService: ĐÃ KẾT NỐI")
                    appendLine("Danh tính tiến trình: $identity")
                    appendLine("PID: $pid")
                    appendLine("INJECT_EVENTS: $permission")
                    append("Chế độ hiện tại: chỉ chẩn đoán, chưa inject thao tác.")
                }
            }.onFailure { error ->
                mutableStatus.value =
                    "Đọc kết quả UserService thất bại: ${error.message ?: "lỗi không xác định"}"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            mutableStatus.value = "Shizuku UserService đã ngắt kết nối."
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        Shizuku.addRequestPermissionResultListener(permissionListener)
        initialized = true
    }

    fun probe(context: Context) {
        initialize(context)

        if (!Shizuku.pingBinder()) {
            mutableStatus.value =
                "Không tìm thấy Shizuku đang chạy. Hãy khởi động Shizuku rồi thử lại."
            return
        }

        if (Shizuku.isPreV11()) {
            mutableStatus.value =
                "Phiên bản Shizuku hiện tại quá cũ. Cần Shizuku API v11 trở lên."
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            mutableStatus.value =
                "Đang yêu cầu quyền Shizuku. Hãy chấp thuận yêu cầu trên màn hình."
            runCatching {
                Shizuku.requestPermission(REQUEST_CODE)
            }.onFailure { error ->
                mutableStatus.value =
                    "Không thể yêu cầu quyền Shizuku: ${error.message ?: "lỗi không xác định"}"
            }
            return
        }

        bindProbeService()
    }

    @Synchronized
    private fun bindProbeService() {
        val context = appContext ?: run {
            mutableStatus.value = "Shizuku probe chưa được khởi tạo."
            return
        }
        if (serviceBound) {
            mutableStatus.value = "Shizuku UserService đang được kết nối."
            return
        }

        val component = ComponentName(
            BuildConfig.APPLICATION_ID,
            ShizukuInputProbeService::class.java.name
        )
        val args = Shizuku.UserServiceArgs(component)
            .daemon(false)
            .processNameSuffix("input_probe")
            .tag("macroandroid_input_probe")
            .version(SERVICE_VERSION)

        runCatching {
            Shizuku.bindUserService(args, serviceConnection)
            serviceBound = true
            mutableStatus.value = "Đang khởi chạy Shizuku UserService..."
        }.onFailure { error ->
            serviceBound = false
            mutableStatus.value =
                "Không thể khởi chạy UserService: ${error.message ?: "lỗi không xác định"}"
        }
    }
}
