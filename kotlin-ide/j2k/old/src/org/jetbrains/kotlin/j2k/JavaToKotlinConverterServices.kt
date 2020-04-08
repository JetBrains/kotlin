/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.j2k.DocCommentConverter
import org.jetbrains.kotlin.idea.j2k.EmptyDocCommentConverter

interface JavaToKotlinConverterServices {
    val referenceSearcher: ReferenceSearcher
    val superMethodsSearcher: SuperMethodsSearcher
    val resolverForConverter: ResolverForConverter
    val docCommentConverter: DocCommentConverter
    val javaDataFlowAnalyzerFacade: JavaDataFlowAnalyzerFacade
}

object EmptyJavaToKotlinServices: JavaToKotlinConverterServices {
    override val referenceSearcher: ReferenceSearcher
        get() = EmptyReferenceSearcher

    override val superMethodsSearcher: SuperMethodsSearcher
        get() = SuperMethodsSearcher.Default

    override val resolverForConverter: ResolverForConverter
        get() = EmptyResolverForConverter

    override val docCommentConverter: DocCommentConverter
        get() = EmptyDocCommentConverter

    override val javaDataFlowAnalyzerFacade: JavaDataFlowAnalyzerFacade
        get() = JavaDataFlowAnalyzerFacade.Default
}

interface SuperMethodsSearcher {
    fun findDeepestSuperMethods(method: PsiMethod): Collection<PsiMethod>

    object Default : SuperMethodsSearcher {
        // use simple findSuperMethods by default because findDeepestSuperMethods requires some service from IDEA
        override fun findDeepestSuperMethods(method: PsiMethod) = method.findSuperMethods().asList()
    }
}
