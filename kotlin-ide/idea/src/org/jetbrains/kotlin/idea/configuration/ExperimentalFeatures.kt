/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.components.JBCheckBox
import org.jdesktop.swingx.VerticalLayout
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.PlatformVersion
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService
import org.jetbrains.kotlin.idea.util.isDev
import org.jetbrains.kotlin.idea.util.isEap
import org.jetbrains.kotlin.idea.util.isSnapshot
import javax.swing.JCheckBox
import javax.swing.JPanel

object ExperimentalFeatures {
    val NewJ2k = RegistryExperimentalFeature(
        title = KotlinBundle.message("configuration.feature.text.new.java.to.kotlin.converter"),
        registryKey = "kotlin.experimental.new.j2k",
        enabledByDefault = true
    )

    val allFeatures: List<ExperimentalFeature> = listOf(
        NewJ2k,
    ) + ExperimentalFeature.EP_NAME.extensionList
}

abstract class ExperimentalFeature {
    abstract val title: String
    abstract var isEnabled: Boolean
    open fun shouldBeShown(): Boolean = true
    open fun onFeatureStatusChanged(enabled: Boolean) {}

    companion object {
        internal var EP_NAME = ExtensionPointName<ExperimentalFeature>("org.jetbrains.kotlin.experimentalFeature")
    }
}

open class RegistryExperimentalFeature(
    override val title: String,
    private val registryKey: String,
    private val enabledByDefault: Boolean
) : ExperimentalFeature() {
    final override var isEnabled
        get() = Registry.`is`(registryKey, enabledByDefault)
        set(value) {
            Registry.get(registryKey).setValue(value)
        }
}

class ExperimentalFeaturesPanel : JPanel(VerticalLayout(5)) {
    private val featuresWithCheckboxes = ExperimentalFeatures.allFeatures.map { feature ->
        FeatureWithCheckbox(
            feature,
            JBCheckBox(feature.title, feature.isEnabled)
        )
    }

    init {
        featuresWithCheckboxes.forEach { (feature, checkBox) ->
            if (feature.shouldBeShown()) {
                add(checkBox)
            }
        }
    }

    private data class FeatureWithCheckbox(
        val feature: ExperimentalFeature,
        val checkbox: JCheckBox
    )

    fun isModified() = featuresWithCheckboxes.any { (feature, checkBox) ->
        feature.isEnabled != checkBox.isSelected
    }

    fun applySelectedChanges() {
        featuresWithCheckboxes.forEach { (feature, checkBox) ->
            if (feature.isEnabled != checkBox.isSelected) {
                feature.isEnabled = checkBox.isSelected
                feature.onFeatureStatusChanged(checkBox.isSelected)
            }
        }
    }

    companion object {
        fun shouldBeShown(): Boolean {
            val version = KotlinCompilerVersion.VERSION
            return isEap(version) || isDev(version) || isSnapshot(version)
        }
    }
}
