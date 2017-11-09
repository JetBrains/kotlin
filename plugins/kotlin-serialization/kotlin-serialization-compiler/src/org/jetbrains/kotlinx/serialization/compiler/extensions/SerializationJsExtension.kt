/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlinx.serialization.compiler.backend.js.SerializableCompanionJsTranslator
import org.jetbrains.kotlinx.serialization.compiler.backend.js.SerializableJsTranslator
import org.jetbrains.kotlinx.serialization.compiler.backend.js.SerializerJsTranslator

class SerializationJsExtension: JsSyntheticTranslateExtension {
    override fun generateClassSyntheticParts(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
        SerializerJsTranslator.translate(declaration, descriptor, translator, context)
        SerializableJsTranslator.translate(declaration, descriptor, translator, context)
        SerializableCompanionJsTranslator.translate(declaration, descriptor, translator, context)
    }
}