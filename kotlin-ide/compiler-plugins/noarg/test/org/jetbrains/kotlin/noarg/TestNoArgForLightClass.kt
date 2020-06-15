/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinProjectDescriptorWithFacet
import org.jetbrains.kotlin.noarg.ide.NO_ARG_ANNOTATION_OPTION_PREFIX
import org.jetbrains.kotlin.psi.KtFile
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

private const val targetClassName = "TargetClassName"
private const val baseClassName = "BaseClassName"
private const val noArgAnnotationName = "HelloNoArg"

@RunWith(JUnit38ClassRunner::class)
class TestNoArgForLightClass : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinProjectDescriptorWithFacet(LanguageVersion.LATEST_STABLE, multiPlatform = false)

    override fun setUp() {
        super.setUp()

        val facet = KotlinFacet.get(module) ?: error { "Facet not found" }
        val configurationArguments = facet.configuration.settings.compilerArguments ?: error { "CompilerArguments not found" }

        configurationArguments.pluginClasspaths = arrayOf("SomeClasspath")
        configurationArguments.pluginOptions = arrayOf("$NO_ARG_ANNOTATION_OPTION_PREFIX$noArgAnnotationName")
    }

    fun testNoArgAnnotation() {
        val file = myFixture.configureByText(
            "A.kt",
            "annotation class $noArgAnnotationName\n"
                    + "@$noArgAnnotationName class $targetClassName(val e: Int)"
        ) as KtFile

        val classes = file.classes
        assertEquals(2, classes.size)

        val targetClass = classes.firstOrNull { it.name == targetClassName }
            ?: error { "Expected class $targetClassName not found" }

        val constructors = targetClass.constructors
        assertEquals(constructors.size, 2)
        assertTrue(constructors.any { it.parameters.isEmpty() })
    }

    fun testNoArgDerivedAnnotation() {
        val file = myFixture.configureByText(
            "A.kt",
            "annotation class $noArgAnnotationName\n"
                    + "@$noArgAnnotationName class $baseClassName(val e: Int)\n"
                    + "class $targetClassName(val k: Int) : $baseClassName(k)"
        ) as KtFile

        val classes = file.classes
        assertEquals(3, classes.size)

        val targetClass = classes.firstOrNull { it.name == targetClassName }
            ?: error { "Expected class $targetClassName not found" }

        val constructors = targetClass.constructors
        assertEquals(constructors.size, 2)
        assertTrue(constructors.any { it.parameters.isEmpty() })
    }
}