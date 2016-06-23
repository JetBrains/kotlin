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

package org.jetbrains.kotlin.annotation

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.annotation.processing.impl.*
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.annotation.processing.Processor

class AnnotationProcessingExtension(
        val generatedOutputDir: File,
        val annotationProcessingClasspath: List<String>
) : AnalysisCompletedHandlerExtension {
    private var annotationProcessingComplete = false

    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingContext: BindingContext,
            files: Collection<KtFile>
    ): AnalysisResult? {
        if (annotationProcessingComplete) {
            return null
        }
        
        val processors = loadAnnotationProcessors(annotationProcessingClasspath)
        if (processors.isEmpty()) return null
        
        val analysisContext = AnalysisContext(hashMapOf())
        analysisContext.analyzeFiles(files)
        
        val options = emptyMap<String, String>()
        
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)
        
        val filer = KotlinFiler(generatedOutputDir)
        val messages = KotlinMessager()
        val types = KotlinTypes(javaPsiFacade, PsiManager.getInstance(project), projectScope)
        val elements = KotlinElements(javaPsiFacade, projectScope)
        
        val processingEnvironment = KotlinProcessingEnvironment(elements, types, messages, options, filer)
        processors.forEach { it.init(processingEnvironment) }
        
        
        // Round 1
        val round1Environment = KotlinRoundEnvironment(analysisContext)
        for (processor in processors) {
            val supportedAnnotationNames = processor.supportedAnnotationTypes.filter { it in analysisContext.annotationsMap }
            if (supportedAnnotationNames.isEmpty()) continue

            val supportedAnnotations = supportedAnnotationNames
                    .map { javaPsiFacade.findClass(it, projectScope)?.let { JeTypeElement(it) } }
                    .filterNotNullTo(hashSetOf())

            processor.process(supportedAnnotations, round1Environment)
        }

        // Round 2
        val round2Environment = KotlinRoundEnvironment(AnalysisContext(hashMapOf()), isProcessingOver = true)
        for (processor in processors) {
            processor.process(emptySet(), round2Environment)
        }

        annotationProcessingComplete = true
        return AnalysisResult.RetryWithAdditionalJavaRoots(bindingContext, module, listOf(generatedOutputDir))
    }
    
    private fun loadAnnotationProcessors(classpath: List<String>): List<Processor> {
        val classLoader = URLClassLoader(classpath.map { File(it).toURI().toURL() }.toTypedArray())
        return ServiceLoader.load(Processor::class.java, classLoader).toList()
    } 
}

internal class AnalysisContext(val annotationsMap: MutableMap<String, MutableList<PsiModifierListOwner>>)

private fun AnalysisContext.analyzeFiles(files: Collection<KtFile>) {
    for (file in files) {
        analyzeFile(file)
    }
}

private fun AnalysisContext.analyzeFile(file: KtFile) {
    val lightClass = file.findFacadeClass()
    
    if (lightClass != null) {
        analyzeDeclaration(lightClass)
    }
    
    for (declaration in file.declarations) {
        if (declaration !is KtClassOrObject) continue
        val clazz = declaration.toLightClass() ?: continue
        analyzeDeclaration(clazz)
    }
}

private fun AnalysisContext.analyzeDeclaration(declaration: PsiElement) {
    if (declaration !is PsiModifierListOwner) return
    
    val annotations = declaration.modifierList?.annotations
    if (annotations != null) {
        for (annotation in annotations) {
            val fqName = annotation.qualifiedName ?: continue
            annotationsMap.getOrPut(fqName, { mutableListOf() }).add(declaration)
        }
    }
    
    if (declaration is PsiClass) {
        declaration.methods.forEach { analyzeDeclaration(it) }
        declaration.fields.forEach { analyzeDeclaration(it) }
        declaration.innerClasses.forEach { analyzeDeclaration(it) }
    }
    
    when (declaration) {
        is KtClassOrObject -> declaration.declarations.forEach { analyzeDeclaration(it) }
        
    }
}