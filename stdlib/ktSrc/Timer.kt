package std.concurrent

import java.util.Timer
import java.util.TimerTask
import java.util.Date

fun Timer.schedule(delay: Long, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    schedule(task, delay)
    return task
}

fun Timer.schedule(time: Date, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    schedule(task, time)
    return task
}

fun Timer.schedule(delay: Long, period: Long, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    schedule(task, delay, period)
    return task
}

fun Timer.schedule(time: Date, period: Long, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    schedule(task, time, period)
    return task
}

fun Timer.scheduleAtFixedRate(delay: Long, period: Long, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    scheduleAtFixedRate(task, delay, period)
    return task
}

fun Timer.scheduleAtFixedRate(time: Date, period: Long, action: TimerTask.()->Unit) : TimerTask {
    val task = createTask(action)
    scheduleAtFixedRate(task, time, period)
    return task
}

fun timer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.lng, period: Long, action: TimerTask.()->Unit) : Timer {
    val timer = if(name == null) Timer(daemon) else Timer(name, daemon)
    timer.schedule(initialDelay, period, action)
    return timer
}

fun timer(name: String? = null, daemon: Boolean = false, startAt: Date, period: Long, action: TimerTask.()->Unit) : Timer {
    val timer = if(name == null) Timer(daemon) else Timer(name, daemon)
    timer.schedule(startAt, period, action)
    return timer
}

fun fixedRateTimer(name: String? = null, daemon: Boolean = false, initialDelay: Long = 0.lng, period: Long, action: TimerTask.()->Unit) : Timer {
    val timer = if(name == null) Timer(daemon) else Timer(name, daemon)
    timer.scheduleAtFixedRate(initialDelay, period, action)
    return timer
}

fun fixedRateTimer(name: String? = null, daemon: Boolean = false, startAt: Date, period : Long, action: TimerTask.()->Unit) : Timer {
    val timer = if(name == null) Timer(daemon) else Timer(name, daemon)
    timer.scheduleAtFixedRate(startAt, period, action)
    return timer
}

private fun createTask(action: TimerTask.()->Unit) : TimerTask = object: TimerTask() {
    override fun run() {
        action()
    }
}