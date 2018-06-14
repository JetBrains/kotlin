/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import global.*
import kotlinx.cinterop.*
import platform.posix.*
import konan.worker.*

inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean = { x -> x == 0} ): Int {
    if (!predicate(this)) {
        throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
    }
    return this
}

data class SharedDataMember(val double: Double)

data class SharedData(val string: String, val int: Int, val member: SharedDataMember)

// Here we access the same shared frozen Kotlin object from multiple threads.
val globalObject: SharedData?
    get() = sharedData.frozenKotlinObject?.asStableRef<SharedData>()?.get()

fun dumpShared(prefix: String): Unit {
    println("""
            $prefix: ${pthread_self()} x=${sharedData.x} f=${sharedData.f} s=${sharedData.string!!.toKString()}
            """.trimIndent())
}

fun main(args: Array<String>) {
    // Arena owning all native allocs.
    val arena = Arena()

    // Assign global data.
    sharedData.x = 239
    sharedData.f = 0.5f
    sharedData.string = "Hello Kotlin!".cstr.getPointer(arena)
    // Here we create detached mutable object, which could be later reattached by another thread.
    sharedData.kotlinObject = konan.worker.detachObjectGraph {
        SharedData("A string", 42, SharedDataMember(2.39))
    }
    // Here we create shared frozen object reference,
    val stableRef = StableRef.create(SharedData("Shared", 239, SharedDataMember(2.71)).freeze())
    sharedData.frozenKotlinObject = stableRef.asCPointer()
    dumpShared("thread1")
    println("frozen is $globalObject")

    // Start a new thread, that sees the variable.
    // memScoped is needed to pass thread's local address to pthread_create().
    memScoped {
        val thread = alloc<pthread_tVar>()
        pthread_create(thread.ptr, null, staticCFunction {
            argC ->
            initRuntimeIfNeeded()
            dumpShared("thread2")
            val kotlinObject = konan.worker.attachObjectGraph<SharedData>(sharedData.kotlinObject)
            val arg = konan.worker.attachObjectGraph<SharedDataMember>(argC)
            println("thread arg is $arg Kotlin object is $kotlinObject frozen is $globalObject")
            // Workaround for compiler issue.
            null as COpaquePointer?
        }, konan.worker.detachObjectGraph { SharedDataMember(3.14)} ).ensureUnixCallResult("pthread_create")
        pthread_join(thread.value, null).ensureUnixCallResult("pthread_join")
    }

    // At this moment we do not need data stored in shared data, so clean up the data
    // and free memory.
    sharedData.string = null
    stableRef.dispose()
    arena.clear()
}
