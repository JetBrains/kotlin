package com.example

import com.google.auto.service.AutoService
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING

@AutoService(Processor::class)
class Processor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = setOf(SomeAnnotation::class.java.canonicalName)

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(MANDATORY_WARNING, "*** AP RUNNING ***' ")
        return true
    }
}
