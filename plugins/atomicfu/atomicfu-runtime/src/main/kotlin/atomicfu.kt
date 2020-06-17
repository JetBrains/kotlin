/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.atomicfu

/**
 * Inline functions that are substituted instead of the corresponding atomic functions defined in `kotlinx.atomicfu`
 * during Js/Ir transformation.
 *
 * Example of transformation:
 * ```
 * val a = atomic(0)
 * a.compareAndSet(expect, update)
 * ```
 * is transformed to:
 * ```
 * var a = 0
 * atomicfu_compareAndSet(expect, update, { return a }, { v: Int -> a.value = v })
 * ```
 */

internal inline fun <T> atomicfu_getValue(`atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): T {
    return `atomicfu$getter`()
}

internal inline fun <T> atomicfu_setValue(value: T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): Unit {
    `atomicfu$setter`(value)
}

internal inline fun <T> atomicfu_lazySet(value: T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): Unit {
    `atomicfu$setter`(value)
}

internal inline fun <T> atomicfu_compareAndSet(expect: T, update: T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): Boolean {
    if (`atomicfu$getter`() == expect) {
        `atomicfu$setter`(update)
        return true
    } else {
        return false
    }
}

internal inline fun <T> atomicfu_getAndSet(value: T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): T {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(value)
    return oldValue
}

internal inline fun atomicfu_getAndIncrement(`atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue + 1)
    return oldValue
}

internal inline fun atomicfu_getAndIncrement(`atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue + 1)
    return oldValue
}

internal inline fun atomicfu_incrementAndGet(`atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    `atomicfu$setter`(`atomicfu$getter`() + 1)
    return `atomicfu$getter`()
}

internal inline fun atomicfu_incrementAndGet(`atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    `atomicfu$setter`(`atomicfu$getter`() + 1)
    return `atomicfu$getter`()
}

internal inline fun atomicfu_getAndDecrement(`atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue - 1)
    return oldValue
}

internal inline fun atomicfu_getAndDecrement(`atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue - 1)
    return oldValue
}

internal inline fun atomicfu_decrementAndGet(`atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    `atomicfu$setter`(`atomicfu$getter`() - 1)
    return `atomicfu$getter`()
}

internal inline fun atomicfu_decrementAndGet(`atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    `atomicfu$setter`(`atomicfu$getter`() - 1)
    return `atomicfu$getter`()
}

internal inline fun atomicfu_getAndAdd(value: Int, `atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue + value)
    return oldValue
}

internal inline fun atomicfu_getAndAdd(value: Long, `atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    val oldValue = `atomicfu$getter`()
    `atomicfu$setter`(oldValue + value)
    return oldValue
}

internal inline fun atomicfu_addAndGet(value: Int, `atomicfu$getter`: () -> Int, `atomicfu$setter`: (Int) -> Unit): Int {
    `atomicfu$setter`(`atomicfu$getter`() + value)
    return `atomicfu$getter`()
}

internal inline fun atomicfu_addAndGet(value: Long, `atomicfu$getter`: () -> Long, `atomicfu$setter`: (Long) -> Unit): Long {
    `atomicfu$setter`(`atomicfu$getter`() + value)
    return `atomicfu$getter`()
}

internal inline fun <T> atomicfu_loop(action: (T) -> Unit, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): Nothing {
    while (true) {
        val cur = `atomicfu$getter`()
        action(cur)
    }
}

internal inline fun <T> atomicfu_update(function: (T) -> T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit) {
    while (true) {
        val cur = `atomicfu$getter`()
        val upd = function(cur)
        if (atomicfu_compareAndSet(cur, upd, `atomicfu$getter`, `atomicfu$setter`)) return
    }
}

internal inline fun <T> atomicfu_getAndUpdate(function: (T) -> T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): T {
    while (true) {
        val cur = `atomicfu$getter`()
        val upd = function(cur)
        if (atomicfu_compareAndSet(cur, upd, `atomicfu$getter`, `atomicfu$setter`)) return cur
    }
}

internal inline fun <T> atomicfu_updateAndGet(function: (T) -> T, `atomicfu$getter`: () -> T, `atomicfu$setter`: (T) -> Unit): T {
    while (true) {
        val cur = `atomicfu$getter`()
        val upd = function(cur)
        if (atomicfu_compareAndSet(cur, upd, `atomicfu$getter`, `atomicfu$setter`)) return upd
    }
}