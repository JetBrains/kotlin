/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaClassInitializerUFunction(
        override val psi: PsiClassInitializer,
        override val parent: UElement
) : UFunction, PsiElementBacked, NoAnnotations, NoModifiers {
    override val kind: UastFunctionKind.UastInitializerKind
        get() = JavaFunctionKinds.STATIC_INITIALIZER

    override val valueParameters: List<UVariable>
        get() = emptyList()

    override val valueParameterCount: Int
        get() = 0

    override val typeParameters: List<UTypeReference>
        get() = emptyList()

    override val typeParameterCount: Int
        get() = 0

    override val returnType: UType?
        get() = null

    override val body by lz { JavaConverter.convert(psi.body, this) }

    override val visibility: UastVisibility
        get() = UastVisibility.LOCAL

    override val nameElement: UElement?
        get() = null

    override val name: String
        get() = "<static>"

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()
}