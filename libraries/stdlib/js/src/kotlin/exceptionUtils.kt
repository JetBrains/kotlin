/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module

@JsName("throwNPE")
private fun throwNPE(message: String) {
    throw NullPointerException(message)
}

@JsName("throwCCE")
private fun throwCCE() {
    throw ClassCastException("Illegal cast")
}

@JsName("throwISE")
private fun throwISE(message: String) {
    throw IllegalStateException(message)
}

@JsName("throwUPAE")
private fun throwUPAE(propertyName: String) {
    throw UninitializedPropertyAccessException("lateinit property ${propertyName} has not been initialized")
}
