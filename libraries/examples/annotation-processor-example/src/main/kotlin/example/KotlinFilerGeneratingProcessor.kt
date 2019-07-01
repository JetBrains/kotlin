/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package example

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

/** Annotation processor that users [javax.annotation.processing.Filer] APIs to generate Kotlin sources. */
class KotlinFilerGeneratingProcessor : AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val annotatedWith = roundEnv.getElementsAnnotatedWith(KotlinFilerGenerated::class.java)

        for (element in annotatedWith) {
            val packageName = processingEnv.elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName.toString()

            processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, packageName, "${simpleName}Generated.kt", element)
                .openWriter().use {
                    it.write(
                        """
                    package $packageName

                    class ${simpleName}Generated
                """.trimIndent()
                    )
                }
        }
        return true
    }

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(KotlinFilerGenerated::class.java.canonicalName)
    }
}