/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package example

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

annotation class Anno

class ExampleAnnotationProcessor : AbstractProcessor() {
    private companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Anno::class.java)
        val kotlinGenerated = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]

        for (element in elements) {
            if (element.kind != ElementKind.CLASS) continue

            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName.toString()

            // generate a Java helper
            val javaFile = processingEnv.filer.createSourceFile("$packageName.${simpleName}Generated")
            javaFile.openWriter().buffered().use { writer ->
                if (packageName.isNotEmpty()) writer.appendLine("package $packageName;")
                writer.appendLine("public class ${simpleName}Generated {")
                writer.appendLine("    public static String getGeneratedMessage() {")
                writer.appendLine("        return \"Generated for $simpleName\";")
                writer.appendLine("    }")
                writer.appendLine("}")
            }

            // generate a Kotlin helper as an extension function
            if (kotlinGenerated != null) {
                File(kotlinGenerated, "${simpleName}Extensions.kt").writer().buffered().use { writer ->
                    if (packageName.isNotEmpty()) writer.appendLine("package $packageName")
                    writer.appendLine("fun $simpleName.generatedExtension(): String = \"${simpleName}: \" + toString()")
                }
            }
        }
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.RELEASE_8
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(Anno::class.java.name)
    override fun getSupportedOptions(): Set<String> = emptySet<String>()
}
