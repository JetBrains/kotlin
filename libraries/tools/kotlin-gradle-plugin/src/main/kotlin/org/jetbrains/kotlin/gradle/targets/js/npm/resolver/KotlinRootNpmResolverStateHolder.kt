/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin

// We use this service as a single storage for duplicated data in configuration cache
internal abstract class KotlinRootNpmResolverStateHolder : BuildService<KotlinRootNpmResolverStateHolder.Parameters> {
    interface Parameters : BuildServiceParameters {
        val plugins: ListProperty<RootResolverPlugin>
        val projectResolvers: MapProperty<String, KotlinProjectNpmResolver>
    }

    var initialized = false

    @Volatile
    var state = KotlinRootNpmResolver.State.CONFIGURING
}