/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("MetadataProperties")

package com.google.dart.compiler.backend.js.ast.metadata

import com.google.dart.compiler.backend.js.ast.*
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.inline.InlineStrategy

var JsName.staticRef: JsNode? by MetadataProperty(default = null)

// TODO: move this to module 'js.inliner' and change dependency on 'frontend' to dependency on 'descriptors'
var JsInvocation.inlineStrategy: InlineStrategy? by MetadataProperty(default = null)

var JsInvocation.descriptor: CallableDescriptor? by MetadataProperty(default = null)

var JsInvocation.psiElement: PsiElement? by MetadataProperty(default = null)

var JsFunction.isLocal: Boolean by MetadataProperty(default = false)

var JsParameter.hasDefaultValue: Boolean by MetadataProperty(default = false)

var JsInvocation.typeCheck: TypeCheck? by MetadataProperty(default = null)

/**
 * For function and lambda bodies indicates what declaration corresponds to.
 * When absent (`null`) on body of a named function, this function is from external JS module.
 */
var JsFunction.functionDescriptor: FunctionDescriptor? by MetadataProperty(default = null)

/**
 * For return statement specifies corresponding target descriptor given by [functionDescriptor].
 * For all JsReturn nodes created by K2JSTranslator, this property is filled, either for local/non-local labeled and non-labeled returns.
 *
 * Absence of this property (expressed as `null`) means that the corresponding JsReturn got from external JS library.
 * In this case we assume that such return can never be non-local.
 */
var JsReturn.returnTarget: FunctionDescriptor? by MetadataProperty(default = null)

var HasMetadata.synthetic: Boolean by MetadataProperty(default = false)

var HasMetadata.sideEffects: Boolean by MetadataProperty(default = true)

enum class TypeCheck {
    TYPEOF,
    INSTANCEOF,
    OR_NULL,
    IS_ANY,
    AND_PREDICATE
}
