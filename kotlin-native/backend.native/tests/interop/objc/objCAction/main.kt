/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import objclib.*

import platform.Foundation.*
import kotlin.test.*
import kotlinx.cinterop.*
import kotlin.concurrent.Volatile

class CondVar {
    @Volatile
    @PublishedApi
    internal var steps = 0

    fun notify() {
        steps += 1
    }

    inline fun <R> runAndWait(block: () -> R) {
        val prev = steps
        val result = block()
        // During block() execution someone somewhere does notify().
        // In this loop we wait to read the result of that notify(), which acts as a synchronization point.
        while (steps <= prev) {}
    }
}

class Incrementor : NSObject() {
    @ObjCOutlet
    var counter: NSNumber = NSNumber.numberWithInt(0)

    val condVar = CondVar()

    @ObjCAction
    fun increment() { // Only Unit is allowed for return value
        println("I'm here to make sure this function generates a frame. Here's an object: ${Any()}")
        counter = NSNumber.numberWithInt(counter.intValue + 1)
        condVar.notify()
    }

    @ObjCAction
    fun incrementBy(amount: NSNumber) { // Only Unit is allowed for return value
        println("I'm here to make sure this function generates a frame. Here's an object: ${Any()}")
        counter = NSNumber.numberWithInt(counter.intValue + amount.intValue)
        condVar.notify()
    }

    @ObjCAction
    fun incrementOtherBy(other: Incrementor, amount: NSNumber) { // Only Unit is allowed for return value
        println("I'm here to make sure this function generates a frame. Here's an object: ${Any()}")
        other.incrementBy(amount)
        condVar.notify()
    }
}

class IncrementorViaObjC {
    val impl = Incrementor()

    var counter: NSNumber
        get() = impl.counter // ObjCOutlet only works for setters - not getters.
        set(value) = setProperty(impl, "counter", value)

    fun increment() = performSelector0(impl, "increment")

    fun incrementBy(amount: NSNumber) = performSelector1(impl, "incrementBy:", amount)

    fun incrementOtherBy(other: Incrementor, amount: NSNumber) = performSelector2(impl, "incrementOtherBy:amount:", other, amount)
}

class IncrementorViaObjCInNewThread {
    val impl = Incrementor()

    var counter: NSNumber
        get() = impl.counter // ObjCOutlet only works for setters - not getters.
        set(value) = setProperty(impl, "counter", value)

    fun increment() = impl.condVar.runAndWait { performSelectorInNewThread0(impl, "increment") }

    fun incrementBy(amount: NSNumber) = impl.condVar.runAndWait { performSelectorInNewThread1(impl, "incrementBy:", amount) }

    fun incrementOtherBy(other: Incrementor, amount: NSNumber) = impl.condVar.runAndWait { performSelectorInNewThread2(impl, "incrementOtherBy:amount:", other, amount) }
}

// Sanity checking that using functions and properties as regular kotlin properties works.
@Test
fun testIncrementorKt() {
    val incrementor = Incrementor()
    assertEquals(0, incrementor.counter.intValue)

    incrementor.increment()
    assertEquals(1, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(3))
    assertEquals(4, incrementor.counter.intValue)

    incrementor.counter = NSNumber.numberWithInt(7)
    assertEquals(7, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(2))
    assertEquals(9, incrementor.counter.intValue)

    val otherIncrementor = Incrementor()
    incrementor.incrementOtherBy(otherIncrementor, NSNumber.numberWithInt(5))
    assertEquals(9, incrementor.counter.intValue)
    assertEquals(5, otherIncrementor.counter.intValue)
}

// Doing everything testIncrementorKt does, but via ObjC dynamic dispatch
@Test
fun testIncrementorObjC() {
    val incrementor = IncrementorViaObjC()
    assertEquals(0, incrementor.counter.intValue)

    incrementor.increment()
    assertEquals(1, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(3))
    assertEquals(4, incrementor.counter.intValue)

    incrementor.counter = NSNumber.numberWithInt(7)
    assertEquals(7, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(2))
    assertEquals(9, incrementor.counter.intValue)

    val otherIncrementor = Incrementor()
    incrementor.incrementOtherBy(otherIncrementor, NSNumber.numberWithInt(5))
    assertEquals(9, incrementor.counter.intValue)
    assertEquals(5, otherIncrementor.counter.intValue)
}

// Doing everything testIncrementorKt does, but via ObjC dynamic dispatch and in a new NSThread
@Test
fun testIncrementorObjCInNewThread() {
    if (!isExperimentalMM()) // Cross-thread stuff doesn't work with the legacy MM
        return

    val incrementor = IncrementorViaObjCInNewThread()
    assertEquals(0, incrementor.counter.intValue)

    incrementor.increment()
    assertEquals(1, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(3))
    assertEquals(4, incrementor.counter.intValue)

    incrementor.counter = NSNumber.numberWithInt(7)
    assertEquals(7, incrementor.counter.intValue)

    incrementor.incrementBy(NSNumber.numberWithInt(2))
    assertEquals(9, incrementor.counter.intValue)

    val otherIncrementor = Incrementor()
    incrementor.incrementOtherBy(otherIncrementor, NSNumber.numberWithInt(5))
    assertEquals(9, incrementor.counter.intValue)
    assertEquals(5, otherIncrementor.counter.intValue)
}

// Mixing Kt and ObjC accesses
@Test
fun testIncrementorMix() {
    val objc = IncrementorViaObjC()
    val kt = objc.impl
    assertEquals(0, kt.counter.intValue)

    kt.increment()
    assertEquals(1, kt.counter.intValue)

    objc.increment()
    assertEquals(2, kt.counter.intValue)

    kt.counter = NSNumber.numberWithInt(7)
    assertEquals(7, kt.counter.intValue)

    objc.increment()
    assertEquals(8, kt.counter.intValue)

    objc.counter = NSNumber.numberWithInt(11)
    assertEquals(11, kt.counter.intValue)

    kt.increment()
    assertEquals(12, kt.counter.intValue)
}

// Mixing Kt and ObjC accesses when ObjC happens in a different thread
@Test
fun testIncrementorMixInNewThread() {
    if (!isExperimentalMM()) // Cross-thread stuff doesn't work with the legacy MM
        return

    val objc = IncrementorViaObjCInNewThread()
    val kt = objc.impl
    assertEquals(0, kt.counter.intValue)

    kt.increment()
    assertEquals(1, kt.counter.intValue)

    objc.increment()
    assertEquals(2, kt.counter.intValue)

    kt.counter = NSNumber.numberWithInt(7)
    assertEquals(7, kt.counter.intValue)

    objc.increment()
    assertEquals(8, kt.counter.intValue)

    objc.counter = NSNumber.numberWithInt(11)
    assertEquals(11, kt.counter.intValue)

    kt.increment()
    assertEquals(12, kt.counter.intValue)
}