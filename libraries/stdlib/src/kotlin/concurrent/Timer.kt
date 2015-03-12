package kotlin.concurrent

import java.util.Timer
import java.util.TimerTask
import java.util.Date

/**
 * Schedules an [action] to be executed after the specified [delay] (expressed in milliseconds).
 */
public fun Timer.schedule(delay: Long, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay)
    return task
}

/**
 * Schedules an [action] to be executed at the specified [time].
 */
public fun Timer.schedule(time: Date, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, time)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting after the specified [delay] (expressed
 * in milliseconds) and with the interval of [period] milliseconds between the end of the previous task
 * and the start of the next one.
 */
public fun Timer.schedule(delay: Long, period: Long, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, delay, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting at the specified [time] and with the
 * interval of [period] milliseconds between the end of the previous task and the start of the next one.
 */
public fun Timer.schedule(time: Date, period: Long, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    schedule(task, time, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting after the specified [delay] (expressed
 * in milliseconds) and with the interval of [period] milliseconds between the start of the previous task
 * and the start of the next one.
 */
public fun Timer.scheduleAtFixedRate(delay: Long, period: Long, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    scheduleAtFixedRate(task, delay, period)
    return task
}

/**
 * Schedules an [action] to be executed periodically, starting at the specified [time] and with the
 * interval of [period] milliseconds between the start of the previous task and the start of the next one.
 */
public fun Timer.scheduleAtFixedRate(time: Date, period: Long, action: TimerTask.() -> Unit): TimerTask {
    val task = timerTask(action)
    scheduleAtFixedRate(task, time, period)
    return task
}

/**
 * Creates a timer that executes the specified [action] periodically, starting after the specified [initialDelay]
 * (expressed in milliseconds) and with the interval of [period] milliseconds between the end of the previous task
 * and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if true, the thread is started as a daemon thread (the VM will exit when only daemon threads are running)
 */
public fun timer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.toLong(), period: Long, action: TimerTask.() -> Unit): Timer {
    val timer = if (name == null) Timer(daemon) else Timer(name, daemon)
    timer.schedule(initialDelay, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting at the specified [startAt] date
 * and with the interval of [period] milliseconds between the end of the previous task and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if true, the thread is started as a daemon thread (the VM will exit when only daemon threads are running)
 */
public fun timer(name: String? = null, daemon: Boolean = false, startAt: Date, period: Long, action: TimerTask.() -> Unit): Timer {
    val timer = if (name == null) Timer(daemon) else Timer(name, daemon)
    timer.schedule(startAt, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting after the specified [initialDelay]
 * (expressed in milliseconds) and with the interval of [period] milliseconds between the start of the previous task
 * and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if true, the thread is started as a daemon thread (the VM will exit when only daemon threads are running)
 */
public fun fixedRateTimer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.toLong(), period: Long, action: TimerTask.() -> Unit): Timer {
    val timer = if (name == null) Timer(daemon) else Timer(name, daemon)
    timer.scheduleAtFixedRate(initialDelay, period, action)
    return timer
}

/**
 * Creates a timer that executes the specified [action] periodically, starting at the specified [startAt] date
 * and with the interval of [period] milliseconds between the start of the previous task and the start of the next one.
 *
 * @param name the name to use for the thread which is running the timer.
 * @param daemon if true, the thread is started as a daemon thread (the VM will exit when only daemon threads are running)
 */
public fun fixedRateTimer(name: String? = null, daemon: Boolean = false, startAt: Date, period: Long, action: TimerTask.() -> Unit): Timer {
    val timer = if (name == null) Timer(daemon) else Timer(name, daemon)
    timer.scheduleAtFixedRate(startAt, period, action)
    return timer
}

/**
 * Wraps the specified [action] in a `TimerTask`.
 */
public fun timerTask(action: TimerTask.() -> Unit): TimerTask = object : TimerTask() {
    public override fun run() {
        action()
    }
}