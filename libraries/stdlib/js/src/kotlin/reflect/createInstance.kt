/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Creates a new instance of the class, calling a constructor which either has no parameters or all parameters of which have
 * a default value. If there are no or many such constructors, an exception is thrown.
 */
@OptIn(JsIntrinsic::class)
@SinceKotlin("1.9")
@ExperimentalJsReflectionCreateInstance
public fun <T : Any> KClass<T>.createInstance(): T {
    val jsClass = js.asDynamic()

    if (jsClass === js("Object")) return js("{}")

    val noArgsConstructor = jsClass.`$metadata$`.unsafeCast<Metadata?>()?.defaultConstructor
        ?: throw IllegalArgumentException("Class \"$simpleName\" should have a single no-arg constructor")

    return if (jsIsEs6() && noArgsConstructor !== jsClass) {
        js("noArgsConstructor.call(jsClass)")
    } else {
        js("new noArgsConstructor()")
    }
}
