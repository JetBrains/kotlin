/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsModule("lit/decorators.js")
@file:JsNonModule
package lit

@JsDecorator
@JsName("customElement")
external annotation class CustomElement(val tagName: String)

@JsDecorator
@JsName("property")
external annotation class Property(/*val options: PropertyDeclaration? = null*/)
