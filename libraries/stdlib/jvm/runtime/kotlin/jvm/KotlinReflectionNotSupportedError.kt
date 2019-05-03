/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import java.lang.Error

public open class KotlinReflectionNotSupportedError : Error {
    constructor() : super("Kotlin reflection implementation is not found at runtime. Make sure you have kotlin-reflect.jar in the classpath")

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
