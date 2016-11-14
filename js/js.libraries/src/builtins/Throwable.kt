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

package kotlin

import kotlin.js.internal.JsError

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(open val message: String?, open val cause: Throwable?) : JsError(message) {
    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    init {
        var e = JsError(message)
        copyOwnProperties(e, this)
        val t: dynamic = this
        t.name = t.constructor.name
    }
}

private fun copyOwnProperties(from: dynamic, to: dynamic) {
    val names: Array<String> = js("Object").getOwnPropertyNames(from);
    for (name in names) {
        to[name] = from[name];
    }
}
