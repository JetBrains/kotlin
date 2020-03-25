/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*

fun ParsingContext.parseSettingsMap(
    path: String,
    values: Map<String, Any?>,
    settingReferences: List<Pair<SettingReference<*, *>, Setting<*, *>>>
): TaskResult<List<Pair<SettingReference<*, *>, Any>>> = settingReferences.mapComputeM { (settingReference, setting) ->
    val settingValue = values[setting.path]
    when {
        settingValue != null -> {
            setting.type.parse(this, settingValue, setting.path).map { listOf(settingReference to it) }
        }
        setting.isRequired -> {
            fail(ParseError(KotlinNewProjectWizardBundle.message("parse.error.no.value.for.key", "$path.${setting.path}")))
        }
        else -> success(emptyList())
    }
}.sequence().map { it.flatten() }
