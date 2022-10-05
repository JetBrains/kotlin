/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsModule("lit")
@file:JsNonModule
package lit

//@JsTemplateStringTag
//external fun html(s: String): Any
// TODO(lit) replace with proper version
external fun html(s: Array<String>): Any

@JsTemplateStringTag
external fun css(s: String): String

open external class LitElement {
    open fun render(): Any // `TemplateResult`?
}
