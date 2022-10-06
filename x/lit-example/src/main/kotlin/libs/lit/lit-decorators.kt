/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsModule("lit/decorators.js")
@file:JsNonModule

@file:Suppress(
//    "ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT",
    "WRONG_EXTERNAL_DECLARATION",
    "WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER",
//    "ANNOTATION_CLASS_MEMBER"
)
package lit

@JsDecorator
@JsName("customElement")
external annotation class CustomElement(val tagName: String)

@JsDecorator
@JsName("property")
external annotation class Property(/*val options: PropertyDeclaration? = null*/)

@JsDecorator
@JsName("property")
external annotation class Property2(val options: PropertyDeclaration = PropertyDeclaration())

@JsObjectLiteral
external annotation class PropertyDeclaration(
    val attribute: Boolean = true,
//    val converter:
    val noAccessor: Boolean = true,
    val reflect: Boolean = true,
    val state: Boolean = true,
//    val type: TypeHint,
//    val hasChanged
)

@JsDecorator
@JsName("property")
external annotation class Property3(
    @JsOptionsLiteralParameter val attribute: Boolean = true,
//    val converter:
    @JsOptionsLiteralParameter val noAccessor: Boolean = true,
    @JsOptionsLiteralParameter val reflect: Boolean = true,
    @JsOptionsLiteralParameter val state: Boolean = true,
//    val type: TypeHint,
//    val hasChanged
)


@JsDecorator
@JsName("property")
external annotation class Property4(val options: PropertyDeclaration = PropertyDeclaration()) {
    constructor(
        @JsOptionsLiteralParameter attribute: Boolean = true,
        @JsOptionsLiteralParameter noAccessor: Boolean = true,
        @JsOptionsLiteralParameter reflect: Boolean = true,
        @JsOptionsLiteralParameter state: Boolean = true,
    )
}
