/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation.processing.impl

import org.jetbrains.kotlin.annotation.AnalysisContext
import org.jetbrains.kotlin.java.model.JeConverter
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

internal class KotlinRoundEnvironment(
        private val context: AnalysisContext,
        private val isProcessingOver: Boolean = false) : RoundEnvironment {
    private var isError = false
    
    override fun getRootElements() = emptySet<Element>()
    
    override fun processingOver() = isProcessingOver

    private fun getElementsAnnotatedWith(fqName: String): Set<Element> {
        val declarations = context.annotationsMap[fqName] ?: return emptySet()
        return hashSetOf<Element>().apply {
            for (declaration in declarations) {
                JeConverter.convert(declaration)?.let { add(it) }
            }
        }
    }

    override fun getElementsAnnotatedWith(a: TypeElement) = getElementsAnnotatedWith(a.qualifiedName.toString())
    override fun getElementsAnnotatedWith(a: Class<out Annotation>) = getElementsAnnotatedWith(a.canonicalName)

    override fun errorRaised() = isError
}