/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.ml

import com.intellij.codeInsight.completion.ml.CompletionEnvironment
import com.intellij.codeInsight.completion.ml.ContextFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.statistics.FileTypeStats

class KotlinContextFeatureProvider : ContextFeatureProvider {
    override fun getName(): String = "kotlin"

    override fun calculateFeatures(environment: CompletionEnvironment): Map<String, MLFeatureValue> {
        val features = mutableMapOf("plugin_version" to MLFeatureValue.categorical(KotlinVersionFakeEnum.VERSION))

        val fileType = environment.parameters.originalFile.virtualFile?.name?.let { FileTypeStats.parseFromFileName(it) }
        if (fileType != null) {
            features["file_type"] = MLFeatureValue.categorical(fileType)
        }

        return features
    }
}

/**
 * We do not have a enum to represent Kotlin Plugin version; because of that we cannot
 * make it a categorical feature for ML completion (`MLFeatureValue.categorical` accepts only enums).
 *
 * This fake enum is used as a workaround to this problem.
 *
 * TODO As soon as there would be a way to pass Kotlin Plugin version without this enum, it should be removed.
 */
private enum class KotlinVersionFakeEnum {
    VERSION;

    override fun toString(): String = KotlinPluginUtil.getPluginVersion()
}
