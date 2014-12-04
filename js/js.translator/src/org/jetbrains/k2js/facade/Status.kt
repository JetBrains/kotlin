/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.facade

import kotlin.platform.platformStatic

public class Status<out T> private (private val _result: T, private val type: Status.Type) {
    class object {
        platformStatic fun fail<T>(): Status<T> = Status<T>(null, Type.Fail)

        platformStatic fun success<T>(value: T): Status<T> = Status(value, Type.Success)
    }

    public val result: T
        get() {
            assert(type == Type.Success) { "Getting result from fail status is illegal" }
            return _result
        }

    public fun isFail(): Boolean = type == Type.Fail

    public fun isSuccess(): Boolean = type == Type.Success

    private enum class Type {
        Fail
        Success
    }
}