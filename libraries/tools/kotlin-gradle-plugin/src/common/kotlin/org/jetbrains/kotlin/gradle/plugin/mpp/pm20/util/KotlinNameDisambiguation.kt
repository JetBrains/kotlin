/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.project.model.KpmFragment

/**
 * Mechanism for disambiguating/scoping names for certain entities (e.g. fragments)
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
interface GradleKpmNameDisambiguation {
    fun disambiguateName(simpleName: String): String
}

/* Default implementation for fragments */

internal fun FragmentNameDisambiguation(module: KpmModule, fragmentName: String): GradleKpmNameDisambiguation {
    return GradleKpmDefaultFragmentNameDisambiguation(module, fragmentName)
}

internal fun FragmentNameDisambiguationOmittingMain(module: KpmModule, fragmentName: String): GradleKpmNameDisambiguation {
    return GradleKpmDefaultFragmentNameDisambiguationOmittingMain(module, fragmentName)
}

private class GradleKpmDefaultFragmentNameDisambiguation(
    private val module: KpmModule,
    private val fragmentName: String
) : GradleKpmNameDisambiguation {
    override fun disambiguateName(simpleName: String): String {
        return KpmFragment.disambiguateName(module, fragmentName, simpleName)
    }
}

private class GradleKpmDefaultFragmentNameDisambiguationOmittingMain(
    private val module: KpmModule,
    private val fragmentName: String
) : GradleKpmNameDisambiguation {
    override fun disambiguateName(simpleName: String): String {
        return KpmFragment.disambiguateNameOmittingMain(module, fragmentName, simpleName)
    }
}

internal fun KpmFragment.disambiguateName(simpleName: String) =
    KpmFragment.disambiguateName(containingModule, fragmentName, simpleName)

internal val KpmFragment.unambiguousNameInProject
    get() = disambiguateName("")

internal fun KpmFragment.Companion.disambiguateName(module: KpmModule, fragmentName: String, simpleName: String) =
    lowerCamelCaseName(fragmentName, module.moduleIdentifier.moduleClassifier ?: GradleKpmModule.MAIN_MODULE_NAME, simpleName)

internal fun KpmFragment.Companion.disambiguateNameOmittingMain(module: KpmModule, fragmentName: String, simpleName: String) =
    lowerCamelCaseName(fragmentName, module.moduleIdentifier.moduleClassifier, simpleName)

