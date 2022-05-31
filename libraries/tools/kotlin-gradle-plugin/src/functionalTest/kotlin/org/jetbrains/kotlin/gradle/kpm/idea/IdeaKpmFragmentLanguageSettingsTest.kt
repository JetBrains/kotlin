/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.kpm.buildIdeaKpmProjectModel
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.assertIsNotEmpty
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.junit.Test
import org.junit.jupiter.api.Assertions
import kotlin.test.assertNotNull

class IdeaKpmFragmentLanguageSettingsTest : AbstractIdeaKpmFragmentsContentTest() {
    @Test
    fun testLanguageFeaturesConfigureFreeArgs() {
        doSetupProject()
        languagesFeaturesAsFreeArgs.forEach { (expectedLanguageFeatures, freeArgsValue) ->
            testLanguageFeaturesDirectArgumentsClosure(expectedLanguageFeatures) {
                it.freeCompilerArgs += freeArgsValue
            }
        }
    }

    private fun testLanguageFeaturesDirectArgumentsClosure(
        expectedLanguageFeatures: IdeaKpmFragmentLanguageFeatures,
        configure: (KotlinCommonOptions) -> Unit
    ) = doTestLanguageFeatures(expectedLanguageFeatures) {
        configure(it.kotlinOptions)
    }

    private fun doTestLanguageFeatures(expectedLanguageFeatures: IdeaKpmFragmentLanguageFeatures, configure: (KotlinCompile<*>) -> Unit) {
        project.tasks.withType(KotlinCompile::class.java).forEach(configure)
        kotlin.buildIdeaKpmProjectModel().assertIsNotEmpty().assertLanguageFeatures(expectedLanguageFeatures)
    }

    private fun IdeaKpmProject.assertLanguageFeatures(expectedLanguageFeatures: IdeaKpmFragmentLanguageFeatures) {
        modules.forEach { kpmModule ->
            kpmModule.fragments.forEach { kpmFragment ->
                val extra = kpmFragment.extras[extrasKeyOf<IdeaKpmFragmentLanguageFeatures>()]
                assertNotNull(extra, "Fragment '${kpmFragment.coordinates}' doesn't contain IdeaKpmFragmentLanguageFeatures extra")
                Assertions.assertIterableEquals(expectedLanguageFeatures.languageFeatures, extra.languageFeatures)
            }
        }
    }

    companion object {

        private val languagesFeaturesAsFreeArgs: Sequence<Pair<IdeaKpmFragmentLanguageFeatures, String>>
            get() = sequenceOf(
                ideaKpmFragmentLanguageFeatures(LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED) to "-Xmulti-platform",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.UnrestrictedBuilderInference to LanguageFeature.State.ENABLED) to "-Xunrestricted-builder-inference",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.UseBuilderInferenceWithoutAnnotation to LanguageFeature.State.ENABLED) to "-Xenable-builder-inference",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.TypeInferenceOnCallsWithSelfTypes to LanguageFeature.State.ENABLED) to "-Xself-upper-bound-inference",
                ideaKpmFragmentLanguageFeatures(
                    LanguageFeature.NewInference to LanguageFeature.State.ENABLED,
                    LanguageFeature.SamConversionPerArgument to LanguageFeature.State.ENABLED,
                    LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to LanguageFeature.State.ENABLED,
                    LanguageFeature.DisableCompatibilityModeForNewInference to LanguageFeature.State.ENABLED,
                ) to "-Xnew-inference",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.ContextReceivers to LanguageFeature.State.ENABLED) to "-Xcontext-receivers",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.InlineClasses to LanguageFeature.State.ENABLED) to "-Xinline-classes",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.SoundSmartCastsAfterTry to LanguageFeature.State.ENABLED) to "-Xlegacy-smart-cast-after-try",
                ideaKpmFragmentLanguageFeatures(
                    LanguageFeature.UseCallsInPlaceEffect to LanguageFeature.State.ENABLED,
                    LanguageFeature.UseReturnsEffect to LanguageFeature.State.ENABLED,
                ) to "-Xeffect-system",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.ReadDeserializedContracts to LanguageFeature.State.ENABLED) to "-Xread-deserialized-contracts",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.ProperIeee754Comparisons to LanguageFeature.State.ENABLED) to "-Xproper-ieee754-comparisons",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition to LanguageFeature.State.ENABLED) to "-Xuse-mixed-named-arguments",
                ideaKpmFragmentLanguageFeatures(LanguageFeature.InferenceCompatibility to LanguageFeature.State.ENABLED) to "-Xinference-compatibility",
                ideaKpmFragmentLanguageFeatures(*LanguageFeature.values().filter { it.kind.enabledInProgressiveMode }
                    .map { it to LanguageFeature.State.ENABLED }.toTypedArray()) to "-progressive"
            )

        private fun ideaKpmFragmentLanguageFeatures(vararg languageFeatures: Pair<LanguageFeature, LanguageFeature.State>) =
            IdeaKpmFragmentLanguageFeaturesImpl(languageFeatures.map { IdeaKpmFragmentLanguageFeatureImpl(it.first.name, it.second.name) })
    }
}