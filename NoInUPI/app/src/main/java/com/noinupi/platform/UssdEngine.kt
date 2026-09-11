package com.noinupi.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.noinupi.domain.UssdEnginePort
import com.noinupi.domain.UssdFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class UssdEngine(private val context: Context) : UssdEnginePort, UssdFrameListener {

    companion object {
        const val DOUBLE_TAP_COOLDOWN_MS = 2_000L
        const val SLOW_WATCH_TIMEOUT_MS = 15_000L
        const val HARD_TIMEOUT_MS = 60_000L
    }

    @Volatile private var sessionId = 0
    @Volatile private var sessionActive = false
    private var frameCounter = 0
    private var lastDialTime = 0L

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var slowWatchJob: Job? = null
    private var hardTimeoutJob: Job? = null

    private val _frames = MutableSharedFlow<UssdFrame>(extraBufferCapacity = 32)
    override val frames: SharedFlow<UssdFrame> = _frames

    override suspend fun dial(code: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastDialTime < DOUBLE_TAP_COOLDOWN_MS) return
        lastDialTime = now

        val service = UssdAccessibilityService.instance
        service?.dismissDialog()
        service?.resetForNewSession()

        sessionActive = true
        service?.sessionActive = true
        service?.frameListener = this
        sessionId++
        frameCounter = 0

        startTimers()

        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${Uri.encode(code)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override suspend fun sendReply(reply: String): Boolean {
        if (!sessionActive) return false
        val service = UssdAccessibilityService.instance ?: return false
        val ok = service.sendReply(reply)
        if (ok) resetSlowWatch()
        return ok
    }

    override suspend fun cancel() {
        terminateSession("Cancelled")
    }

    override suspend fun dismissDialog(): Boolean {
        return UssdAccessibilityService.instance?.dismissDialog() ?: false
    }

    override fun getSessionId(): Int = sessionId

    override fun isServiceEnabled(): Boolean = UssdAccessibilityService.instance != null

    override fun onFrame(text: String, isMenu: Boolean, isTerminal: Boolean) {
        if (!sessionActive) return
        frameCounter++
        _frames.tryEmit(
            UssdFrame(text, isMenu, isTerminal, sessionId, frameCounter)
        )
        resetSlowWatch()
    }

    override fun onSessionEnded(reason: String) {
        if (!sessionActive) return
        frameCounter++
        _frames.tryEmit(
            UssdFrame(reason, false, true, sessionId, frameCounter)
        )
        sessionActive = false
    }

    private fun startTimers() {
        slowWatchJob?.cancel()
        hardTimeoutJob?.cancel()

        slowWatchJob = scope.launch {
            delay(SLOW_WATCH_TIMEOUT_MS)
            if (sessionActive) terminateSession("USSD timed out")
        }

        hardTimeoutJob = scope.launch {
            delay(HARD_TIMEOUT_MS)
            if (sessionActive) terminateSession("USSD session timed out")
        }
    }

    private fun resetSlowWatch() {
        if (!sessionActive) return
        slowWatchJob?.cancel()
        slowWatchJob = scope.launch {
            delay(SLOW_WATCH_TIMEOUT_MS)
            if (sessionActive) terminateSession("USSD timed out")
        }
    }

    private fun terminateSession(reason: String) {
        if (!sessionActive) return
        sessionActive = false
        UssdAccessibilityService.instance?.sessionActive = false
        UssdAccessibilityService.instance?.dismissDialog()
        slowWatchJob?.cancel()
        hardTimeoutJob?.cancel()
        slowWatchJob = null
        hardTimeoutJob = null

        frameCounter++
        _frames.tryEmit(
            UssdFrame(reason, false, true, sessionId, frameCounter)
        )
    }
}
