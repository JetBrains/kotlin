/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module

@JsName("throwNPE")
internal fun throwNPE(message: String) {
    throw NullPointerException(message)
}

@JsName("throwCCE")
internal fun throwCCE() {
    throw ClassCastException("Illegal cast")
}

@JsName("throwISE")
internal fun throwISE(message: String) {
    throw IllegalStateException(message)
}

@JsName("throwIAE")
internal fun throwIAE(message: String) {
    throw IllegalArgumentException(message)
}

@JsName("throwUPAE")
internal fun throwUPAE(propertyName: String) {
    throw UninitializedPropertyAccessException("lateinit property ${propertyName} has not been initialized")
}
