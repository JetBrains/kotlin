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

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKDeclaration
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKJavaField
import org.jetbrains.kotlin.j2k.tree.JKJavaMethod
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaPrimitiveTypeImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtFunctionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtPropertyImpl

class JavaFieldToKotlinPropertyConversion : TransformerBasedConversion() {

    override fun visitElement(element: JKElement): JKElement = element.also { it.transformChildren(this, null) }

    override fun visitJavaField(javaField: JKJavaField): JKDeclaration {
        somethingChanged = true

        return JKKtPropertyImpl(javaField.modifierList, javaField.type, javaField.name, javaField.initializer)
    }
}

class JavaMethodToKotlinFunctionConversion : TransformerBasedConversion() {
    override fun visitElement(element: JKElement): JKElement = element.also { it.transformChildren(this, null) }

    override fun visitJavaMethod(javaMethod: JKJavaMethod): JKDeclaration {
        somethingChanged = true
        return JKKtFunctionImpl(
            JKJavaPrimitiveTypeImpl.BOOLEAN,
            javaMethod.name,
            javaMethod.valueArguments,
            javaMethod.block,
            javaMethod.modifierList
        )
    }
}