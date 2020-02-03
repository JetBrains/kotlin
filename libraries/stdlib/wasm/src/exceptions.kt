/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

public open class Error constructor(message: String?, cause: Throwable?) : Throwable(message, cause ?: null) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

public open class AssertionError private constructor(message: String?, cause: Throwable?) : Error(message, cause) {
    constructor() : this(null)
    constructor(message: String?) : this(message, null)
    constructor(message: Any?) : this(message.toString(), message as? Throwable)
}
