/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package compiler

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.project.modelx.Attribute
import org.jetbrains.kotlin.project.modelx.languageSetting.AnyLanguageSettingValue
import org.jetbrains.kotlin.project.modelx.plainBuildSystem.KpmFileStructure

typealias CompilerArgumentsContributors<T> = Map<Class<out CommonCompilerArguments>, CommonCompilerArguments.(T) -> Unit>

class Config(
    val kpmFileStructure: KpmFileStructure,
    val attributeContributors: CompilerArgumentsContributors<Pair<Attribute.Key, Attribute>>,
    val settingsContributors: CompilerArgumentsContributors<Pair<String, AnyLanguageSettingValue>>
)