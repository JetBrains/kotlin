/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin.
 */
@Suppress("NOT_DOCUMENTED")
public external interface Console {
    public fun dir(o: Any): Unit
    public fun error(vararg o: Any?): Unit
    public fun info(vararg o: Any?): Unit
    public fun log(vararg o: Any?): Unit
    public fun warn(vararg o: Any?): Unit
}

/**
 * Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin.
 */
public external val console: Console
