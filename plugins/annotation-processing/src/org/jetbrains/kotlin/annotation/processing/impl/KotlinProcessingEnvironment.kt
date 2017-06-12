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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import org.jetbrains.kotlin.java.model.util.disposeAll
import org.jetbrains.kotlin.java.model.util.toDisposable
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion

class KotlinProcessingEnvironment(
        elements: KotlinElements,
        types: KotlinTypes,
        messager: KotlinMessager,
        options: Map<String, String>,
        filer: KotlinFiler,
        
        processors: List<Processor>,
        
        project: Project,
        psiManager: PsiManager,
        javaPsiFacade: JavaPsiFacade,
        projectScope: GlobalSearchScope,
        bindingContext: BindingContext,
        appendJavaSourceRootsHandler: (List<File>) -> Unit
) : ProcessingEnvironment, Disposable {
    private val elements = elements.toDisposable()
    private val types = types.toDisposable()
    private val messager = messager.toDisposable()
    private val filer = filer.toDisposable()
    internal val processors = processors.toDisposable()
    
    internal val project = project.toDisposable()
    internal val psiManager = psiManager.toDisposable()
    internal val javaPsiFacade = javaPsiFacade.toDisposable()
    internal val projectScope = projectScope.toDisposable()
    internal val bindingContext = bindingContext.toDisposable()

    private val registry = ServiceManager.getService(project, JeElementRegistry::class.java).toDisposable()
    internal val appendJavaSourceRootsHandler = appendJavaSourceRootsHandler.toDisposable()
    private val options = Collections.unmodifiableMap(options).toDisposable()

    override fun dispose() {
        types().dispose()
        elements().dispose()
        registry().dispose()
        disposeAll(elements, types, messager, filer, processors,
                   project, psiManager, javaPsiFacade, projectScope, bindingContext,
                   appendJavaSourceRootsHandler, options)
    }

    override fun getElementUtils() = elements()
    override fun getTypeUtils() = types()
    override fun getMessager() = messager()
    override fun getLocale() = Locale.getDefault()
    override fun getSourceVersion() = SourceVersion.RELEASE_8
    override fun getOptions() = options()
    override fun getFiler() = filer()
}