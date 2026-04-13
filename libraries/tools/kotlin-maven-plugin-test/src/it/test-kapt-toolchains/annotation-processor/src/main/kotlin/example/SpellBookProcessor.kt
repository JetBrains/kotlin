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

annotation class SpellCaster

class SpellBookProcessor : AbstractProcessor() {
    private companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.getElementsAnnotatedWith(SpellCaster::class.java)) {
            if (element.kind != ElementKind.CLASS) continue

            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName.toString()

            val javaFile = processingEnv.filer.createSourceFile("$packageName.${simpleName}SpellBook")
            javaFile.openWriter().buffered().use { writer ->
                if (packageName.isNotEmpty()) writer.appendLine("package $packageName;")
                writer.appendLine("public class ${simpleName}SpellBook {")
                writer.appendLine("    public static String favoriteSpell() { return \"Expelliarmus from $simpleName\"; }")
                writer.appendLine("}")
            }

            val kotlinGenerated = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION]
            if (kotlinGenerated != null) {
                File(kotlinGenerated, "${simpleName}Extensions.kt").writer().buffered().use { writer ->
                    if (packageName.isNotEmpty()) writer.appendLine("package $packageName")
                    writer.appendLine("fun $simpleName.castSpell(): String = \"$simpleName casts!\"")
                }
            }
        }
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(SpellCaster::class.java.name)
}
