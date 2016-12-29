/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("CloseableKt")
@file:JvmVersion
package kotlin.io

import java.io.Closeable
import kotlin.internal.*

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this [Closeable] resource.
 * @return the result of [block] function invoked on this resource.
 */
@InlineOnly
public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Throwable) {
        closed = true
        this?.closeSuppressed(e)
        throw e
    } finally {
        if (this != null && !closed) {
            close()
        }
    }
}

@SinceKotlin("1.1")
@PublishedApi
internal fun Closeable.closeSuppressed(cause: Throwable) {
    try {
        close()
    } catch (closeException: Throwable) {
        // on Java 7 we should call
        IMPLEMENTATIONS.addSuppressed(cause, closeException)
    }
}