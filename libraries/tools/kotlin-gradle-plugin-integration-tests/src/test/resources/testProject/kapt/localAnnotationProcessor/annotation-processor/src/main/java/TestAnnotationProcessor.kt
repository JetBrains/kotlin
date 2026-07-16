package org.kotlin.annotationProcessor

import org.yanex.takenoko.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.*
import javax.tools.StandardLocation

@Target(AnnotationTarget.CLASS)
annotation class TestAnnotation

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("org.kotlin.annotationProcessor.TestAnnotation")
@SupportedOptions(TestAnnotationProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class TestAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation::class.java)
        if (annotatedElements.isEmpty()) return false

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        val generatedKtFile = kotlinFile("test.generated") {
            for (element in annotatedElements) {
                val typeElement = element.toTypeElementOrNull() ?: continue

                property("simpleClassName") {
                    receiverType(typeElement.qualifiedName.toString())
                    getterExpression("this::class.java.simpleName")
                }
            }
        }

        File(kaptKotlinGeneratedDir, "testGenerated.kt").apply {
            parentFile.mkdirs()
            writeText(generatedKtFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
        }

        // print warning processingEnv.messager.printMessage(WARNING, "Additional warning message from AP")

        processingEnv.filer
            .createResource(
                StandardLocation.SOURCE_OUTPUT,
                "test.generated",
                "AnotherGenerated.kt",
            ).openWriter().use {
                it.write("package test.generated;   class AnotherGenerated")
            }
        processingEnv.filer
            .createResource(StandardLocation.CLASS_OUTPUT, "abc", "helloWorld.txt")
            .openWriter()
            .use {
                it.write("Hello, world!")
            }

        return true
    }

    fun Element.toTypeElementOrNull(): TypeElement? {
        if (this !is TypeElement) {
            processingEnv.messager.printMessage(ERROR, "Invalid element type, class expected", this)
            return null
        }

        return this
    }
}