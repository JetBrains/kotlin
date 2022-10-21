/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.languageSetting

import compiler.CompilerArgumentsContributors
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

sealed interface LanguageSetting {
    val key: String
}

typealias AnyLanguageSettingValue = Any?
//interface CompilerSettingValue<T> {
//    val setting: CompilerSetting
//    val value: T
//}

data class SimpleLanguageSetting<in A, T>(
    override val key: String,
    val description: String? = null,
    val consistencyRule: ConsistencyRelation<T>,
    val contributor: A.(T) -> Unit,
    val serializer: LSValueSerializer<T>
) : LanguageSetting

fun defaultLanguageSettingsContributors(): CompilerArgumentsContributors<Pair<String, AnyLanguageSettingValue>> = mapOf(
    contributor<K2JVMCompilerArguments>(commonLanguageSettings + jvmLanguageSettings),
    contributor<K2JSCompilerArguments>(commonLanguageSettings + jsLanguageSettings)
)

fun defaultLanguageSettingsSerializers() = (commonLanguageSettings + jvmLanguageSettings + jsLanguageSettings)
    .mapValues { it.value.serializer as LSValueSerializer<Any?> }

private inline fun <reified T: CommonCompilerArguments> contributor(
    settings: Map<String, SimpleLanguageSetting<*, *>>
): Pair<Class<T>, CommonCompilerArguments.(Pair<String, AnyLanguageSettingValue>) -> Unit> = Pair(
    first = T::class.java,
    second = { (key, value) ->
        val setting = settings[key] ?: error("Unknown language setting $key")
        val contributor = setting.contributor as CommonCompilerArguments.(Any?) -> Unit

        contributor.invoke(this, value)
    }
)