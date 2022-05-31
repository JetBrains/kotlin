/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal fun IdeaKpmFragmentLanguageFeatures(compilerArguments: CommonCompilerArguments?): IdeaKpmFragmentLanguageFeatures =
    compilerArguments?.configureLanguageFeatures(MessageCollector.NONE)
        ?.entries.orEmpty()
        .map { (feature, state) -> IdeaKpmFragmentLanguageFeatureImpl(feature.name, state.name) }
        .let { IdeaKpmFragmentLanguageFeaturesImpl(it) }