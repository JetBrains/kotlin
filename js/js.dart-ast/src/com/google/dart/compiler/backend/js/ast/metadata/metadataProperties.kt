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

package com.google.dart.compiler.backend.js.ast.metadata

import com.google.dart.compiler.backend.js.ast.*
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.inline.InlineStrategy

public var JsName.staticRef: JsNode? by MetadataProperty(default = null)

// TODO: move this to module 'js.inliner' and change dependency on 'frontend' to dependency on 'descriptors'
public var JsInvocation.inlineStrategy: InlineStrategy? by MetadataProperty(default = null)

public var JsInvocation.descriptor: CallableDescriptor? by MetadataProperty(default = null)

public var JsInvocation.psiElement: PsiElement? by MetadataProperty(default = null)

public var JsFunction.isLocal: Boolean by MetadataProperty(default = false)

public var JsParameter.hasDefaultValue: Boolean by MetadataProperty(default = false)

public var JsInvocation.typeCheck: TypeCheck? by MetadataProperty(default = null)

public enum class TypeCheck {
    TYPEOF,
    INSTANCEOF
}