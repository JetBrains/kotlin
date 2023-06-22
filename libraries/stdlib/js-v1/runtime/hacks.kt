/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("WRONG_EXTERNAL_DECLARATION")
package kotlin.js

import kotlin.annotation.AnnotationTarget.*

@Retention(AnnotationRetention.BINARY)
@Target(FILE, CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
internal external annotation class JsName(val name: String)

internal external annotation class native

external fun js(code: String): dynamic