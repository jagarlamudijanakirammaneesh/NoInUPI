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

class UssdEngine(
    private val context: Context
) : UssdEnginePort, UssdFrameListener {

    private var sessionId = 0
    private var sessionActive = false
    private var frameCounter = 0
    private var lastDialTime = 0L

    private val scope =
        CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var timeoutJob: Job? = null

    private val _frames =
        MutableSharedFlow<UssdFrame>(extraBufferCapacity = 32)

    override val frames: SharedFlow<UssdFrame> = _frames

    companion object {
        private const val DOUBLE_TAP_COOLDOWN = 2000L
        private const val SESSION_TIMEOUT = 30000L
    }

    override suspend fun dial(code: String) {
        val now = SystemClock.elapsedRealtime()

        if (now - lastDialTime < DOUBLE_TAP_COOLDOWN) {
            return
        }

        lastDialTime = now

        val service = UssdAccessibilityService.instance

        service?.dismissDialog()
        service?.resetForNewSession()

        timeoutJob?.cancel()

        sessionId++
        frameCounter = 0
        sessionActive = true

        service?.sessionActive = true
        service?.frameListener = this

        timeoutJob = scope.launch {
            delay(SESSION_TIMEOUT)

            if (sessionActive) {
                terminateSession("USSD session timed out")
            }
        }

        val intent = Intent(
            Intent.ACTION_CALL,
            Uri.parse("tel:${Uri.encode(code)}")
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    override suspend fun sendReply(reply: String): Boolean {
        if (!sessionActive) {
            return false
        }

        val service =
            UssdAccessibilityService.instance
                ?: return false

        return service.sendReply(reply)
    }

    override suspend fun cancel() {
        if (!sessionActive) {
            return
        }

        terminateSession("User cancelled")
    }

    override suspend fun dismissDialog(): Boolean {
        val service =
            UssdAccessibilityService.instance
                ?: return false

        return service.dismissDialog()
    }

    override fun getSessionId(): Int {
        return sessionId
    }

    override fun isServiceEnabled(): Boolean {
        return UssdAccessibilityService.instance != null
    }

    override fun onFrame(
        text: String,
        isMenu: Boolean,
        isTerminal: Boolean
    ) {
        if (!sessionActive) {
            return
        }

        frameCounter++

        _frames.tryEmit(
            UssdFrame(
                text = text,
                isMenu = isMenu,
                isTerminal = isTerminal,
                sessionId = sessionId,
                frameId = frameCounter
            )
        )
    }

    override fun onSessionEnded(reason: String) {
        if (!sessionActive) {
            return
        }

        frameCounter++

        _frames.tryEmit(
            UssdFrame(
                text = reason,
                isMenu = false,
                isTerminal = true,
                sessionId = sessionId,
                frameId = frameCounter
            )
        )

        sessionActive = false
    }

    private fun terminateSession(reason: String) {
        if (!sessionActive) {
            return
        }

        sessionActive = false

        timeoutJob?.cancel()
        timeoutJob = null

        val service =
            UssdAccessibilityService.instance

        service?.sessionActive = false
        service?.dismissDialog()

        frameCounter++

        _frames.tryEmit(
            UssdFrame(
                text = reason,
                isMenu = false,
                isTerminal = true,
                sessionId = sessionId,
                frameId = frameCounter
            )
        )
    }
}