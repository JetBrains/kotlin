/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnEnv
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnResolution

// We use this service as a single storage for duplicated data in configuration cache
internal abstract class KotlinRootNpmResolverStateHolder : BuildService<KotlinRootNpmResolverStateHolder.Parameters> {
    interface Parameters : BuildServiceParameters {
        val plugins: ListProperty<RootResolverPlugin>
        val projectResolvers: MapProperty<String, KotlinProjectNpmResolver>
        val packageManager: Property<NpmApi>
        val yarnEnvironment: Property<YarnEnv>
        val npmEnvironment: Property<NpmEnvironment>
        val yarnResolutions: ListProperty<YarnResolution>
        val taskRequirements: Property<TasksRequirements>

        // pulled up from compilation resolver since it was failing with ClassNotFoundException on deserialization, see KT-49061
        val packageJsonHandlers: MapProperty<String, List<PackageJson.() -> Unit>>
    }

    var initialized = false

    @Volatile
    var state = KotlinRootNpmResolver.State.CONFIGURING
}