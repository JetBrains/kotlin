package com.example

import com.google.auto.service.AutoService
import org.intellij.lang.annotations.Language
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING
import javax.tools.StandardLocation

@AutoService(Processor::class)
class Processor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = setOf(SomeAnnotation::class.java.canonicalName)

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (annotations.isNotEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Writing StringFactory")
            processingEnv.filer.createClassFile("com.example.StringFactory", *annotations.toTypedArray())
                .openOutputStream()
                .use { output ->
                    javaClass.classLoader.getResourceAsStream("StringFactory.class")!!
                        .use { it.copyTo(output) }
                }
        }

        for (element in roundEnv.getElementsAnnotatedWith(SomeAnnotation::class.java)) {
            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName
            val name = "Generated${element.simpleName}"
            val file = processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                packageName,
                "$name.kt",
                element
            )
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Writing $name")
            file.openWriter().use { writer ->
                writer.write(
                    //language=kotlin
                    """
                    package $packageName
                    
                    object $name {
                      init {
                        println(StringFactory.generateString())
                      }
                    }
                    """.trimIndent()
                )
            }
        }

        return true
    }
}
