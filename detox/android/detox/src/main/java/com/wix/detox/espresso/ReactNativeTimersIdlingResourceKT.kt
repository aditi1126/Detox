package com.wix.detox.espresso

import android.support.test.espresso.IdlingResource
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.Timing
import org.joor.Reflect
import java.util.*

const val BUSY_WINDOW_THRESHOLD = 1500

class TimerReflected(timer: Any) {
    private var reflected = Reflect.on(timer)

    val isRepeating: Boolean
        get() = reflected.field("mRepeat").get()

    val interval: Int
        get() = reflected.field("mInterval").get()

    val targetTime: Long
        get() = reflected.field("mTargetTime").get()
}

class ReactNativeTimersIdlingResourceKT(private val reactContext: ReactContext) : IdlingResource {
    override fun getName() = ""

    override fun isIdleNow(): Boolean {
        val now = System.nanoTime() / 1000000L
        val timingModule = reactContext.getNativeModule(Timing::class.java)
        val timersQueue: PriorityQueue<Any> = Reflect.on(timingModule).field("mTimers").get()

        val nextTimer = timersQueue.peek()
        nextTimer?.let {
            return !isTimerInBusyWindow(it, now) && !hasBusyTimers(timersQueue, now)
        }
        return true
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
    }

    private fun isTimerInBusyWindow(timer: Any, now: Long): Boolean {
        val timerReflected = TimerReflected(timer)
        return when {
            timerReflected.isRepeating -> false
            timerReflected.targetTime < now -> false
            timerReflected.interval > BUSY_WINDOW_THRESHOLD -> false
            else -> true
        }
    }

    private fun hasBusyTimers(timersQueue: PriorityQueue<Any>, now: Long): Boolean {
        timersQueue.forEach {
            if (isTimerInBusyWindow(it, now)) {
                return true
            }
        }
        return false
    }
}
