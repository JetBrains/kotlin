/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MetadataProperties")

package org.jetbrains.kotlin.js.backend.ast.metadata

import org.jetbrains.kotlin.js.backend.ast.*

var JsName.localAlias: LocalAlias? by MetadataProperty(default = null)

data class LocalAlias(val name: JsName, val tag: String?)

var JsName.specialFunction: SpecialFunction? by MetadataProperty(default = null)

var JsExpression.localAlias: JsImportedModule? by MetadataProperty(default = null)

var JsInvocation.isInline: Boolean? by MetadataProperty(default = null)

var JsNameRef.isJsCall: Boolean by MetadataProperty(default = false)

var JsNameRef.isInline: Boolean? by MetadataProperty(default = null)

var JsFunction.isLocal: Boolean by MetadataProperty(default = false)

var JsParameter.hasDefaultValue: Boolean by MetadataProperty(default = false)

var JsVars.exportedPackage: String? by MetadataProperty(default = null)

var JsExpressionStatement.exportedTag: String? by MetadataProperty(default = null)

var HasMetadata.constant: Boolean by MetadataProperty(default = false)
var HasMetadata.synthetic: Boolean by MetadataProperty(default = false)

var HasMetadata.isInlineClassBoxing: Boolean by MetadataProperty(default = false)
var HasMetadata.isInlineClassUnboxing: Boolean by MetadataProperty(default = false)

var HasMetadata.isGeneratorFunction: Boolean by MetadataProperty(default = false)

var HasMetadata.sideEffects: SideEffectKind by MetadataProperty(default = SideEffectKind.AFFECTS_STATE)

/**
 * Denotes a suspension call-site that is to be processed by coroutine transformer.
 * More clearly, denotes invocation that should immediately return from coroutine state machine
 */
var JsExpression.isSuspend: Boolean by MetadataProperty(default = false)

var JsName.imported by MetadataProperty(default = false)

enum class SideEffectKind {
    AFFECTS_STATE,
    DEPENDS_ON_STATE,
    PURE
}

enum class SpecialFunction(val suggestedName: String) {
    DEFINE_INLINE_FUNCTION("defineInlineFunction"),
    WRAP_FUNCTION("wrapFunction"),
    TO_BOXED_CHAR("toBoxedChar"),
    UNBOX_CHAR("unboxChar"),
    SUSPEND_CALL("suspendCall"),
    COROUTINE_RESULT("coroutineResult"),
    COROUTINE_CONTROLLER("coroutineController"),
    COROUTINE_RECEIVER("coroutineReceiver"),
    SET_COROUTINE_RESULT("setCoroutineResult"),
    GET_KCLASS("getKClass"),
    GET_REIFIED_TYPE_PARAMETER_KTYPE("getReifiedTypeParameterKType")
}
