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

package konan.worker

import konan.internal.ExportForCppRuntime

/**
 * Exception thrown whenever freezing is not possible.
 */
public class FreezingException() : RuntimeException()

/**
 * Exception thrown whenever we attempt to mutate frozen objects.
 */
public class InvalidMutabilityException() : RuntimeException()

/**
 * Freezes object subgraph reachable from this object. Frozen objects can be freely
 * shared between threads/workers.
 */
fun <T> T.freeze(): T {
    freezeInternal(this)
    return this
}

// TODO: Remove.
fun <T> T.freezeAllowCycles(): T {
    try {
        freezeInternal(this)
    } catch (t: FreezingException) {
        return this
    }
    return this
}

val Any?.isFrozen
    get() = isFrozenInternal(this)

@SymbolName("Kotlin_Worker_freezeInternal")
internal external fun freezeInternal(it: Any?)

@SymbolName("Kotlin_Worker_isFrozenInternal")
internal external fun isFrozenInternal(it: Any?): Boolean

@ExportForCppRuntime
internal fun ThrowFreezingException(): Nothing = throw FreezingException()

@ExportForCppRuntime
internal fun ThrowInvalidMutabilityException(): Nothing = throw InvalidMutabilityException()
