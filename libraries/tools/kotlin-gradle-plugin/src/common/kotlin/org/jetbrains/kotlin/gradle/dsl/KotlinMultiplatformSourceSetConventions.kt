/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.NamedDomainObjectProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

@KotlinGradlePluginPublicDsl
interface KotlinMultiplatformSourceSetConventions {
    // region Common Source Set Accessors
    // endregion

    // region Non-Native Source Set Accessors
    // endregion

    // region Native Source Set Accessors
    // endregion

    operator fun NamedDomainObjectProvider<KotlinSourceSet>.invoke(
        configure: KotlinSourceSet.() -> Unit,
    ): Unit = get().run(configure)

    fun NamedDomainObjectProvider<KotlinSourceSet>.dependencies(
        handler: KotlinDependencyHandler.() -> Unit,
    ): Unit = get().dependencies(handler)

    fun NamedDomainObjectProvider<KotlinSourceSet>.languageSettings(
        configure: LanguageSettingsBuilder.() -> Unit,
    ): Unit = this { languageSettings(configure) }
}
