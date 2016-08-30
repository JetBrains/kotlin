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

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class KotlinProcessingEnvironment(
        private val elements: Elements,
        private val types: Types,
        private val messager: KotlinMessager,
        options: Map<String, String>,
        private val filer: KotlinFiler,
        
        internal val processors: List<Processor>,
        
        internal val project: Project,
        internal val psiManager: PsiManager,
        internal val javaPsiFacade: JavaPsiFacade,
        internal val projectScope: GlobalSearchScope,
        internal val appendJavaSourceRootsHandler: (List<File>) -> Unit
) : ProcessingEnvironment {
    private val options = Collections.unmodifiableMap(options) 
    
    override fun getElementUtils() = elements
    override fun getTypeUtils() = types
    override fun getMessager() = messager
    override fun getLocale() = Locale.getDefault()
    override fun getSourceVersion() = SourceVersion.RELEASE_8
    override fun getOptions() = options
    override fun getFiler() = filer
}