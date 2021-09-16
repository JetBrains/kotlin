/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package example

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.KClass

annotation class Anno

class ExampleAnnotationProcessor : AbstractProcessor() {
    private companion object {
        val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Anno::class.java)
        val kotlinGenerated = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]

        for (element in elements) {
            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName.toString()

            if (kotlinGenerated != null && element.kind == ElementKind.CLASS) {
                File(kotlinGenerated, "$simpleName.kt").writer().buffered().use {
                    it.appendLine("package $packageName")
                    it.appendLine("fun $simpleName.customToString() = \"$simpleName: \" + toString()")
                }
            }
        }

        return true
    }

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6
    override fun getSupportedAnnotationTypes() = setOf(Anno::class.java.name)
    override fun getSupportedOptions() = emptySet<String>()
}