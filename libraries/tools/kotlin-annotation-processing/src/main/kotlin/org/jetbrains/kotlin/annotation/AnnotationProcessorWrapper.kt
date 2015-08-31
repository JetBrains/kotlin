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

package org.jetbrains.kotlin.annotation

import java.io.File
import java.io.IOException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.properties.Delegates

public class AnnotationProcessorStub : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>?, roundEnv: RoundEnvironment?) = true
}

abstract class AnnotatedElementDescriptor(public val classFqName: String)

data class AnnotatedClassDescriptor(classFqName: String) : AnnotatedElementDescriptor(classFqName)
data class AnnotatedMethodDescriptor(classFqName: String, public val methodName: String) : AnnotatedElementDescriptor(classFqName)
data class AnnotatedConstructorDescriptor(classFqName: String) : AnnotatedElementDescriptor(classFqName)
data class AnnotatedFieldDescriptor(classFqName: String, public val fieldName: String) : AnnotatedElementDescriptor(classFqName)

public abstract class AnnotationProcessorWrapper(
        private val processorFqName: String,
        private val taskQualifier: String
) : Processor {

    private companion object {
        val KAPT_ANNOTATION_OPTION = "kapt.annotations"
    }

    private val processor: Processor by Delegates.lazy {
        try {
            val instance = Class.forName(processorFqName).newInstance() as? Processor
            instance ?: throw IllegalArgumentException("Instance has a wrong type: $processorFqName")
        }
        catch (e: Exception) {
            AnnotationProcessorStub()
        }
    }

    private var processingEnv: ProcessingEnvironment by Delegates.notNull()

    private var kotlinAnnotationsProvider: KotlinAnnotationProvider by Delegates.notNull()

    private var roundCounter = 0

    override fun getCompletions(
            element: Element?,
            annotation: AnnotationMirror?,
            member: ExecutableElement?,
            userText: String?
    ): MutableIterable<Completion> {
        return processor.getCompletions(element, annotation, member, userText)
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        this.processingEnv = processingEnv

        if (processor is AnnotationProcessorStub) {
            processingEnv.err("Can't instantiate processor $processorFqName")
            return
        }

        val annotationsFilePath = processingEnv.getOptions().get(KAPT_ANNOTATION_OPTION)
        val annotationsFile = if (annotationsFilePath != null) File(annotationsFilePath) else null
        kotlinAnnotationsProvider = if (annotationsFile != null && annotationsFile.exists()) {
            FileKotlinAnnotationProvider(annotationsFile)
        }
        else {
            EmptyKotlinAnnotationsProvider()
        }

        processor.init(processingEnv)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        val supportedAnnotations = processor.getSupportedAnnotationTypes().toMutableSet()
        supportedAnnotations.add("__gen.KotlinAptAnnotation")
        return supportedAnnotations
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return processor.getSupportedSourceVersion()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        roundCounter += 1

        val roundEnvironmentWrapper = RoundEnvironmentWrapper(
                processingEnv, roundEnv, roundCounter, kotlinAnnotationsProvider)
        processor.process(annotations, roundEnvironmentWrapper)
        return false
    }

    override fun getSupportedOptions(): MutableSet<String> {
        val supportedOptions = processor.getSupportedOptions().toHashSet()
        supportedOptions.add(KAPT_ANNOTATION_OPTION)
        return supportedOptions
    }

    private fun ProcessingEnvironment.err(message: String) {
        getMessager().printMessage(Diagnostic.Kind.ERROR, message)
    }

}