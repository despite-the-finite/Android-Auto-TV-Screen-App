package com.despitethefinite.carscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.despitethefinite.carscreen.car.CarDisplay

/**
 * Bridge between the phone UI and the car-side display. Both run on the main thread of the
 * same process, so a plain singleton is enough. Commands issued while the car screen is not
 * connected are remembered and replayed when it connects.
 */
object ProjectionController {

    data class Status(val active: Boolean, val width: Int = 0, val height: Int = 0)

    private val mutableStatus = MutableLiveData(Status(active = false))
    val status: LiveData<Status> get() = mutableStatus

    private var display: CarDisplay? = null

    /** Last URL the user asked for; the car screen opens it when it connects. */
    var lastUrl: String = WebConfig.HOME_URL
        private set

    fun attach(target: CarDisplay, width: Int, height: Int) {
        display = target
        mutableStatus.value = Status(active = true, width = width, height = height)
    }

    fun detach(target: CarDisplay) {
        if (display === target) {
            display = null
            mutableStatus.value = Status(active = false)
        }
    }

    fun load(url: String) {
        lastUrl = url
        display?.load(url)
    }

    fun typeText(text: String) = display?.evaluateJs(WebConfig.jsType(text))
    fun pressEnter() = display?.evaluateJs(WebConfig.JS_ENTER)
    fun play() = display?.evaluateJs(WebConfig.JS_PLAY)
    fun pause() = display?.evaluateJs(WebConfig.JS_PAUSE)
    fun back() = display?.back()
    fun reload() = display?.reload()
}
