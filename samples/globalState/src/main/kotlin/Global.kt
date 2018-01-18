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

fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean = { x -> x == 0} ): Int {
    if (!predicate(this)) {
        throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
    }
    return this
}

fun dumpShared(prefix: String) =
        println("$prefix: ${pthread_self().rawValue} x=${sharedData.x} f=${sharedData.f} s=${sharedData.string!!.toKString()}")

fun main(args: Array<String>) {
    // Arena owning all native allocs.
    val arena = Arena()

    // Assign global data.
    sharedData.x = 239
    sharedData.f = 0.5f
    sharedData.string = "Hello Kotlin!".cstr.getPointer(arena)
    dumpShared("thread1")

    // Start a new thread, that sees the variable.
    // memScoped is needed to pass thread's local address to pthread_create().
    memScoped {
        val thread = alloc<pthread_tVar>()
        pthread_create(thread.ptr, null, staticCFunction {
            _ ->
            initRuntimeIfNeeded()
            dumpShared("thread2")
            // Workaround for compiler issue.
            null as COpaquePointer?
        }, null).ensureUnixCallResult("pthread_create")
        pthread_join(thread.value, null).ensureUnixCallResult("pthread_join")
    }

    // At this moment we do not need data stored in shared data, so clean up the data
    // and free memory.
    sharedData.string = null
    arena.clear()
}
