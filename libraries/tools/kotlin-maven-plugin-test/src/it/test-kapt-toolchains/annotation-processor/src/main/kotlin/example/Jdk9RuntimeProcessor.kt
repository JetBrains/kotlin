/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package example

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

annotation class RequiresJdk9Runtime

/**
 * Annotation processor that uses [java.lang.StackWalker] (JDK 9+) at runtime.
 * If loaded on JDK 8, [process] will throw [NoClassDefFoundError].
 * Used to verify that kapt runs the processor on the toolchain JDK, not JAVA_HOME.
 */
@SupportedAnnotationTypes("example.RequiresJdk9Runtime")
class Jdk9RuntimeProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.getElementsAnnotatedWith(RequiresJdk9Runtime::class.java)) {
            if (element.kind != ElementKind.CLASS) continue

            // This call requires JDK 9+ at runtime
            val caller = StackWalker.getInstance().callerClass

            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName.toString()

            val javaFile = processingEnv.filer.createSourceFile("$packageName.${simpleName}Jdk9Generated")
            javaFile.openWriter().buffered().use { writer ->
                if (packageName.isNotEmpty()) writer.appendLine("package $packageName;")
                writer.appendLine("public class ${simpleName}Jdk9Generated {")
                writer.appendLine("    public static String info() { return \"Processed by Jdk9RuntimeProcessor\"; }")
                writer.appendLine("}")
            }
        }
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
}
