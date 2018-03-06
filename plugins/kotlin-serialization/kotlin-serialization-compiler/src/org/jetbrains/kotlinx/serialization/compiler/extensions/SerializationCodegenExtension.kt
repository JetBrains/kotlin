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

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerialInfoCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializableCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializableCompanionCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializerCodegenImpl

class SerializationCodegenExtension : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        SerialInfoCodegenImpl.generateSerialInfoImplBody(codegen)
        SerializableCodegenImpl.generateSerializableExtensions(codegen)
        SerializerCodegenImpl.generateSerializerExtensions(codegen)
        SerializableCompanionCodegenImpl.generateSerializableExtensions(codegen)
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}