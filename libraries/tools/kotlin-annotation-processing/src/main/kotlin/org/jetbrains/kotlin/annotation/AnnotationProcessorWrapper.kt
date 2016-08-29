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
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.properties.Delegates

class AnnotationProcessorStub : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>?, roundEnv: RoundEnvironment?) = true
}

abstract class AnnotationProcessorWrapper(
        private val processorFqName: String,
        private val taskQualifier: String
) : Processor {

    private companion object {
        val KAPT_ANNOTATION_OPTION = "kapt.annotations"
        val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"

        /*
        * If no Java sources are provided, javac finishes without running Annotation Processing.
        * This is not what we want, so we generate a special class for this case
        * with a field annotated with KAPT_SPECIAL_ANNOTATION (see AnnotationProcessingManager.generateJavaHackFile).
        */
        val KAPT_SPECIAL_ANNOTATION = "__gen.KotlinAptAnnotation"
    }

    private val processor: Processor by lazy {
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

    private var handledAnnotationTypes: Set<String> = emptySet()

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

        val annotationsFilePath = processingEnv.options[KAPT_ANNOTATION_OPTION]
        val annotationsFile = if (annotationsFilePath != null) File(annotationsFilePath) else null
        kotlinAnnotationsProvider = if (annotationsFile != null && annotationsFile.exists()) {
            KotlinAnnotationProvider(annotationsFile)
        }
        else {
            KotlinAnnotationProvider()
        }

        processor.init(processingEnv)

        handledAnnotationTypes = this.supportedAnnotationTypes
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        val supportedAnnotations = processor.supportedAnnotationTypes.toMutableSet()
        supportedAnnotations.add(KAPT_SPECIAL_ANNOTATION)
        return supportedAnnotations
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return processor.supportedSourceVersion
    }

    override fun process(annotations: Set<TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        roundCounter += 1

        val roundEnvironmentWrapper = RoundEnvironmentWrapper(
                processingEnv, roundEnv, roundCounter, kotlinAnnotationsProvider)

        val wrappedAnnotations = annotations?.toMutableSet() ?: hashSetOf<TypeElement>()
        val existingFqNames = wrappedAnnotations.mapTo(hashSetOf<String>()) { it.qualifiedName.toString() }

        if (roundCounter == 1) {
            for (annotationFqName in kotlinAnnotationsProvider.annotatedKotlinElements.keys) {
                if (annotationFqName in existingFqNames
                        || annotationFqName == KAPT_SPECIAL_ANNOTATION
                        || annotationFqName !in handledAnnotationTypes // Filter out irrelevant annotations) continue
                ) continue
                existingFqNames.add(annotationFqName)
                processingEnv.elementUtils.getTypeElement(annotationFqName)?.let { wrappedAnnotations += it }
            }
        }

        processor.process(wrappedAnnotations, roundEnvironmentWrapper)
        return false
    }

    override fun getSupportedOptions(): MutableSet<String> {
        val supportedOptions = processor.supportedOptions.toHashSet()
        supportedOptions.add(KAPT_ANNOTATION_OPTION)
        supportedOptions.add(KAPT_KOTLIN_GENERATED_OPTION)
        return supportedOptions
    }

    private fun ProcessingEnvironment.err(message: String) {
        messager.printMessage(Diagnostic.Kind.ERROR, message)
    }

}