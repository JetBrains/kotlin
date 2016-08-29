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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.annotation.processing.diagnostic.ErrorsAnnotationProcessing
import org.jetbrains.kotlin.annotation.processing.impl.*
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.internal.getAnnotationsWithInherited
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.annotation.processing.Processor
import javax.tools.Diagnostic

class ClasspathBasedAnnotationProcessingExtension(
        val annotationProcessingClasspath: List<File>,
        generatedSourcesOutputDir: File,
        classesOutputDir: File,
        javaSourceRoots: List<File>,
        verboseOutput: Boolean
) : AbstractAnnotationProcessingExtension(generatedSourcesOutputDir, classesOutputDir, javaSourceRoots, verboseOutput) {
    override fun loadAnnotationProcessors(): List<Processor> {
        val classLoader = URLClassLoader(annotationProcessingClasspath.map { it.toURI().toURL() }.toTypedArray())
        return ServiceLoader.load(Processor::class.java, classLoader).toList()
    }
}

abstract class AbstractAnnotationProcessingExtension(
        val generatedSourcesOutputDir: File,
        val classesOutputDir: File,
        val javaSourceRoots: List<File>,
        val verboseOutput: Boolean
) : AnalysisCompletedHandlerExtension {
    private var annotationProcessingComplete = false
    private val messager = KotlinMessager()

    private inline fun log(message: () -> String) {
        if (verboseOutput) messager.printMessage(Diagnostic.Kind.OTHER, "Kapt: " + message())
    }
    
    override fun analysisCompleted(
            project: Project,
            module: ModuleDescriptor,
            bindingTrace: BindingTrace,
            files: Collection<KtFile>
    ): AnalysisResult? {
        if (annotationProcessingComplete) {
            return null
        }

        val processors = loadAnnotationProcessors()
        if (processors.isEmpty()) {
            log { "No annotation processors detected, exiting" }
            return null
        }
        
        log { "Analysing Kotlin files: " + files.map { it.virtualFile.path } }
        
        val analysisContext = AnalysisContext(hashMapOf())
        analysisContext.analyzeFiles(files)
        
        log { "Analysing Java source roots: $javaSourceRoots" }
        
        val psiManager = PsiManager.getInstance(project)
        for (javaSourceRoot in javaSourceRoots) {
            javaSourceRoot.walk().filter { it.isFile && it.extension == "java" }.forEach {
                val vFile = StandardFileSystems.local().findFileByPath(it.absolutePath)
                if (vFile != null) {
                    val javaFile = psiManager.findFile(vFile) as? PsiJavaFile
                    if (javaFile != null) {
                        analysisContext.analyzeFile(javaFile)
                    }
                }
            }
        }
        
        val options = emptyMap<String, String>()
        log { "Options: $options" }
        
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)
        
        val filer = KotlinFiler(generatedSourcesOutputDir, classesOutputDir)
        val types = KotlinTypes(javaPsiFacade, PsiManager.getInstance(project), projectScope)
        val elements = KotlinElements(javaPsiFacade, projectScope)
        
        val processingEnvironment = KotlinProcessingEnvironment(elements, types, messager, options, filer)
        processors.forEach { it.init(processingEnvironment) }
        
        log { "Initialized processors: " + processors.joinToString { it.javaClass.name } }
        
        log { "Starting round 1" }
        val round1Environment = KotlinRoundEnvironment(analysisContext)
        for (processor in processors) {
            val supportedAnnotationNames = processor.supportedAnnotationTypes
            val supportsAnyAnnotation = supportedAnnotationNames.contains("*")
            
            val applicableAnnotationNames = when (supportsAnyAnnotation) {
                true -> analysisContext.annotationsMap.keys
                false -> processor.supportedAnnotationTypes.filter { it in analysisContext.annotationsMap } 
            }
            
            if (applicableAnnotationNames.isEmpty()) {
                log { "Skipping processor " + processor.javaClass.name + ": no relevant annotations" }
                continue
            }

            val applicableAnnotations = applicableAnnotationNames
                    .map { javaPsiFacade.findClass(it, projectScope)?.let { JeTypeElement(it) } }
                    .filterNotNullTo(hashSetOf())
            
            log {
                val annotationNames = applicableAnnotations.joinToString { it.qualifiedName.toString() }
                "Processing with " + processor.javaClass.name + " (annotations: " + annotationNames + ")"
            }

            processor.process(applicableAnnotations, round1Environment)
        }

        log { "Starting round 2 (final)" }
        val round2Environment = KotlinRoundEnvironment(AnalysisContext(hashMapOf()), isProcessingOver = true)
        for (processor in processors) {
            processor.process(emptySet(), round2Environment)
        }
        
        fun Int.count(noun: String) = if (this == 1) "$this $noun" else "$this ${noun}s"
        log { "Annotation processing complete, ${messager.errorCount.count("error")}, ${messager.warningCount.count("warning")}" }
        
        if (messager.errorCount != 0) {
            val reportFile = files.firstOrNull()
            if (reportFile != null) {
                bindingTrace.report(ErrorsAnnotationProcessing.ANNOTATION_PROCESSING_ERROR.on(reportFile))
            }
            
            // Do not restart analysis
            return null
        }

        annotationProcessingComplete = true
        return AnalysisResult.RetryWithAdditionalJavaRoots(bindingTrace.bindingContext, module, listOf(generatedSourcesOutputDir))
    }

    protected abstract fun loadAnnotationProcessors(): List<Processor>
}

internal class AnalysisContext(annotationsMap: MutableMap<String, MutableList<PsiModifierListOwner>>) {
    private val mutableAnnotationsMap = annotationsMap
    
    val annotationsMap: Map<String, List<PsiModifierListOwner>>
        get() = mutableAnnotationsMap
    
    fun analyzeFiles(files: Collection<KtFile>) {
        for (file in files) {
            analyzeFile(file)
        }
    }

    fun analyzeFile(file: KtFile) {
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

    fun analyzeFile(file: PsiJavaFile) {
        file.classes.forEach { analyzeDeclaration(it) }
    }

    fun analyzeDeclaration(declaration: PsiElement) {
        if (declaration !is PsiModifierListOwner) return

        for (annotation in declaration.getAnnotationsWithInherited()) {
            val fqName = annotation.qualifiedName ?: return
            mutableAnnotationsMap.getOrPut(fqName, { mutableListOf() }).add(declaration)
        }

        if (declaration is PsiClass) {
            declaration.methods.forEach { analyzeDeclaration(it) }
            declaration.fields.forEach { analyzeDeclaration(it) }
            declaration.innerClasses.forEach { analyzeDeclaration(it) }
        }

        if (declaration is PsiMethod) {
            for (parameter in declaration.parameterList.parameters) {
                analyzeDeclaration(parameter)
            }
        }
    }
}