/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import javax.inject.Inject

/**
 * Base interface for Kotlin targets that have a compilation target that produces binaries.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
// dev note: currently this is only relevant for Native, JS, Wasm.
interface HasBinaries<out T : DomainObjectSet<*>> {
    /**
     * The container of all Kotlin binaries.
     */
    val binaries: T
}

abstract class KotlinTargetWithBinaries<T : KotlinCompilation<Any>, out R : DomainObjectSet<*>> @Inject constructor(
    project: Project,
    platformType: KotlinPlatformType,
) : KotlinOnlyTarget<T>(project, platformType), HasBinaries<R> {
    fun binaries(configure: R.() -> Unit) {
        binaries.configure()
    }

    fun binaries(configure: Action<@UnsafeVariance R>) {
        configure.execute(binaries)
    }
}
