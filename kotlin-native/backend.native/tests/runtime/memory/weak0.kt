/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package runtime.memory.weak0

import kotlin.test.*
import kotlin.native.ref.*

data class Data(val s: String)

fun localWeak(): WeakReference<Data>  {
    val x = Data("Hello")
    val weak = WeakReference(x)
    println(weak.get())
    return weak
}

fun multiWeak(): Array<WeakReference<Data>>  {
    val x = Data("Hello")
    val weaks = Array(100, { WeakReference(x) } )
    weaks.forEach {
        it -> if (it.get()?.s != "Hello") throw Error("bad reference")
    }
    return weaks
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test fun runTest() {
    val weak = localWeak()
    kotlin.native.runtime.GC.collect()
    val value = weak.get()
    println(value?.toString())

    val weaks = multiWeak()

    kotlin.native.runtime.GC.collect()

    weaks.forEach {
        it -> if (it.get()?.s != null) throw Error("not null")
    }
    println("OK")
}