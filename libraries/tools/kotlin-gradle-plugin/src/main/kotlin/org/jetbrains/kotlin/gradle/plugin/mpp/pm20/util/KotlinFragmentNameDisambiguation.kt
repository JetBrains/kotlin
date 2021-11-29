/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.KotlinModuleFragment

internal typealias FragmentNameDisambiguation = KotlinFragmentNameDisambiguation

internal fun FragmentNameDisambiguation(module: KotlinModule, fragmentName: String): FragmentNameDisambiguation {
    return DefaultKotlinFragmentNameDisambiguation(module, fragmentName)
}

/**
 * Mechanism for disambiguating/scoping names for a certain fragment.
 * e.g. Certain fragments might want to create a configuration called 'api'. However, the name scope
 * of Gradle configurations is bound to the Gradle project which requires providing different names of mentioned configurations
 * for FragmentFoo and FragmentBar.
 *
 * In such case a disambiguation _could_ produce:
 *
 * ```kotlin
 * // in main module
 * fragmentFoo.disambiguateName("api") == "fragmentFooApi"
 * fragmentBar.disambiguateName("api") == "fragmentBarApi"
 *
 * // in test module
 * fragmentFoo.disambiguateName("api") == "fragmentFooTestApi"
 * fragmentBar.disambiguateName("api") == "fragmentBarTestApi"
 * ```
 */
interface KotlinFragmentNameDisambiguation {
    fun disambiguateName(simpleName: String): String
}

private class DefaultKotlinFragmentNameDisambiguation(
    private val module: KotlinModule,
    private val fragmentName: String
) : KotlinFragmentNameDisambiguation {
    override fun disambiguateName(simpleName: String): String {
        return KotlinModuleFragment.disambiguateName(module, fragmentName, simpleName)
    }
}

internal fun KotlinModuleFragment.disambiguateName(simpleName: String) =
    KotlinModuleFragment.disambiguateName(containingModule, fragmentName, simpleName)

internal fun KotlinModuleFragment.Companion.disambiguateName(module: KotlinModule, fragmentName: String, simpleName: String) =
    lowerCamelCaseName(fragmentName, module.moduleIdentifier.moduleClassifier ?: KotlinGradleModule.MAIN_MODULE_NAME, simpleName)


