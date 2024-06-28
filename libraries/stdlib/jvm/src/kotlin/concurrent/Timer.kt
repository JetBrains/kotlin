/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TimersKt")
package kotlin.concurrent

import java.util.Date
import java.util.Timer
import java.util.TimerTask

/**
 * Schedules an [action] to be executed after the specified [delay] (expressed in milliseconds).
 */
@kotlin.internal.InlineOnly
public inline fun Timer.schedule(delay: Long, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay)
    return task
}

/**
 * Schedules an [action] to be executed at the specified [time].
 */
@kotlin.internal.InlineOnly
public inline fun Timer.schedule(time: Date, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, time)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting after the specified [delay] (expressed
 * in milliseconds) and with the interval of [period] milliseconds between the end of the previous task
 * and the start of the next one.
 */
@kotlin.internal.InlineOnly
public inline fun Timer.schedule(delay: Long, period: Long, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting at the specified [time] and with the
 * interval of [period] milliseconds between the end of the previous task and the start of the next one.
 */
@kotlin.internal.InlineOnly
public inline fun Timer.schedule(time: Date, period: Long, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, time, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting after the specified [delay] (expressed
 * in milliseconds) and with the interval of [period] milliseconds between the start of the previous task
 * and the start of the next one.
 */
@kotlin.internal.InlineOnly
public inline fun Timer.scheduleAtFixedRate(delay: Long, period: Long, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    scheduleAtFixedRate(task, delay, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting at the specified [time] and with the
 * interval of [period] milliseconds between the start of the previous task and the start of the next one.
 */
@kotlin.internal.InlineOnly
public inline fun Timer.scheduleAtFixedRate(time: Date, period: Long, crossinline action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    scheduleAtFixedRate(task, time, period)
    return task
}


// exposed as public
@PublishedApi
internal fun timer(name: String?, daemon: Boolean): Timer = if (name == null) Timer(daemon) else Timer(name, daemon)

/**
 * Creates a timer that executes the specified [action] periodically, starting after the specified [initialDelay]
 * (expressed in milliseconds) and with the interval of [period] milliseconds between the end of the previous task
 * and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if `true`, the thread is started as a daemon thread (the VM will exit when only daemon threads are running).
 */
@kotlin.internal.InlineOnly
public inline fun timer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.toLong(), period: Long, crossinline action: TimerTask.() -> Unit): Timer {
    val timer = timer(name, daemon)
    timer.schedule(initialDelay, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting at the specified [startAt] date
 * and with the interval of [period] milliseconds between the end of the previous task and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if `true`, the thread is started as a daemon thread (the VM will exit when only daemon threads are running).
 */
@kotlin.internal.InlineOnly
public inline fun timer(name: String? = null, daemon: Boolean = false, startAt: Date, period: Long, crossinline action: TimerTask.() -> Unit): Timer {
    val timer = timer(name, daemon)
    timer.schedule(startAt, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting after the specified [initialDelay]
 * (expressed in milliseconds) and with the interval of [period] milliseconds between the start of the previous task
 * and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if `true`, the thread is started as a daemon thread (the VM will exit when only daemon threads are running).
 */
@kotlin.internal.InlineOnly
public inline fun fixedRateTimer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.toLong(), period: Long, crossinline action: TimerTask.() -> Unit): Timer {
    val timer = timer(name, daemon)
    timer.scheduleAtFixedRate(initialDelay, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting at the specified [startAt] date
 * and with the interval of [period] milliseconds between the start of the previous task and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if `true`, the thread is started as a daemon thread (the VM will exit when only daemon threads are running).
 */
@kotlin.internal.InlineOnly
public inline fun fixedRateTimer(name: String? = null, daemon: Boolean = false, startAt: Date, period: Long, crossinline action: TimerTask.() -> Unit): Timer {
    val timer = timer(name, daemon)
    timer.scheduleAtFixedRate(startAt, period, action)
    return timer
}

/**
 * Wraps the specified [action] in a [TimerTask].
 */
@kotlin.internal.InlineOnly
public inline fun timerTask(crossinline action: TimerTask.() -> Unit): TimerTask = object : TimerTask() {
    override fun run() = action()
}