package com.macro.app.input

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableStatus = MutableStateFlow(
        "Chưa kiểm tra Shizuku. Nhấn nút kiểm tra để bắt đầu."
    )
    val status: StateFlow<String> = mutableStatus.asStateFlow()

    private var appContext: Context? = null
    private var initialized = false

    // Binding is asynchronous: only onServiceConnected means it is connected.
    @Volatile private var bindingInProgress = false
    @Volatile private var serviceConnected = false

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindProbeService()
                } else {
                    mutableStatus.value =
                        "Bạn chưa cấp quyền Shizuku cho MacroAndroid. Có thể cấp quyền rồi kiểm tra lại."
                }
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (!serviceConnected && !bindingInProgress) {
            mutableStatus.value =
                "Đã phát hiện Shizuku. Nhấn “Kiểm tra Shizuku” để chạy chẩn đoán."
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        bindingInProgress = false
        serviceConnected = false
        mutableStatus.value =
            "Kết nối Shizuku đã mất. Hãy khởi động lại Shizuku rồi kiểm tra lại."
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bindingInProgress = false
            val probe = IShizukuInputProbe.Stub.asInterface(binder)
            if (probe == null || binder?.isBinderAlive != true) {
                serviceConnected = false
                mutableStatus.value =
                    "Shizuku đã phản hồi nhưng Binder UserService không hợp lệ."
                return
            }

            serviceConnected = true
            runCatching {
                val uid = probe.processUid
                val pid = probe.processPid
                val hasInjectEvents = probe.hasInjectEventsPermission()
                val identity = when (uid) {
                    0 -> "root (UID 0)"
                    2000 -> "shell/ADB (UID 2000)"
                    else -> "UID $uid"
                }
                val permission =
                    if (hasInjectEvents) "ĐƯỢC CẤP" else "KHÔNG ĐƯỢC CẤP"

                mutableStatus.value = buildString {
                    appendLine("Shizuku UserService: ĐÃ KẾT NỐI")
                    appendLine("Danh tính tiến trình: $identity")
                    appendLine("PID: $pid")
                    appendLine("INJECT_EVENTS: $permission")
                    append(
                        "Chỉ chẩn đoán quyền; chưa có injector và chưa chứng minh được hai luồng cảm ứng độc lập."
                    )
                }
            }.onFailure { error ->
                mutableStatus.value =
                    "Đã kết nối UserService nhưng đọc kết quả thất bại: ${error.message ?: "lỗi không xác định"}"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bindingInProgress = false
            serviceConnected = false
            mutableStatus.value =
                "Shizuku UserService đã ngắt kết nối. Hãy kiểm tra lại."
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (initialized) return

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListener(binderReceivedListener, mainHandler)
        Shizuku.addBinderDeadListener(binderDeadListener, mainHandler)
        initialized = true
    }

    fun probe(context: Context) {
        initialize(context)

        if (!Shizuku.pingBinder()) {
            mutableStatus.value =
                "Không tìm thấy Shizuku đang chạy. Hãy khởi động Shizuku rồi nhấn kiểm tra lại."
            return
        }

        if (Shizuku.isPreV11()) {
            mutableStatus.value =
                "Phiên bản Shizuku hiện tại quá cũ. Cần Shizuku API v11 trở lên."
            return
        }

        if (serviceConnected) {
            mutableStatus.value =
                "Shizuku UserService đã kết nối. Kết quả chẩn đoán đang được giữ trên màn hình."
            return
        }
        if (bindingInProgress) {
            mutableStatus.value = "Đang chờ Shizuku UserService kết nối..."
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            mutableStatus.value =
                "Đang yêu cầu quyền Shizuku. Hãy chấp thuận hộp thoại quyền rồi quay lại ứng dụng."
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

        if (!Shizuku.pingBinder()) {
            mutableStatus.value =
                "Shizuku Binder không còn hoạt động. Hãy khởi động lại Shizuku."
            return
        }
        if (serviceConnected) {
            mutableStatus.value = "Shizuku UserService đã kết nối."
            return
        }
        if (bindingInProgress) {
            mutableStatus.value = "Đang chờ Shizuku UserService kết nối..."
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

        bindingInProgress = true
        mutableStatus.value = "Đang yêu cầu Shizuku khởi chạy UserService..."
        runCatching {
            Shizuku.bindUserService(args, serviceConnection)
        }.onFailure { error ->
            bindingInProgress = false
            serviceConnected = false
            mutableStatus.value =
                "Không thể khởi chạy UserService: ${error.message ?: "lỗi không xác định"}"
        }
    }
}
