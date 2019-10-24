/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import com.intellij.lang.jvm.JvmModifier
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.allopen.ide.ALL_OPEN_ANNOTATION_OPTION_PREFIX
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinProjectDescriptorWithFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class TestAllOpenForLightClass : KotlinLightCodeInsightFixtureTestCase() {

    companion object {
        val allOpenAnnotationName = AbstractAllOpenDeclarationAttributeAltererExtension.ANNOTATIONS_FOR_TESTS.first()
        const val targetClassName = "TargetClassName"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinProjectDescriptorWithFacet(LanguageVersion.LATEST_STABLE, multiPlatform = false)

    override fun setUp() {
        super.setUp()

        val facet = KotlinFacet.get(module) ?: error { "Facet not found" }
        val configurationArguments = facet.configuration.settings.compilerArguments ?: error { "CompilerArguments not found" }

        configurationArguments.pluginClasspaths = arrayOf("SomeClasspath")
        configurationArguments.pluginOptions = arrayOf("$ALL_OPEN_ANNOTATION_OPTION_PREFIX$allOpenAnnotationName")
    }

    fun testAllOpenAnnotation() {
        val file = myFixture.configureByText(
            "A.kt",
            "annotation class $allOpenAnnotationName\n"
                    + "@$allOpenAnnotationName class $targetClassName(val e: Int)\n {"
                    + "  fun a() {}\n"
                    + "  val b = 32\n"
                    + "}"


        ) as KtFile

        val classes = file.classes
        assertEquals(2, classes.size)

        val targetClass = classes.firstOrNull { it.name == targetClassName }
            ?: error { "Expected class $targetClassName not found" }

        assertFalse(targetClass.hasModifier(JvmModifier.FINAL))

        targetClass.methods
            .filter { !it.isConstructor }
            .forEach {
                assertFalse(it.hasModifier(JvmModifier.FINAL))
            }
    }
}