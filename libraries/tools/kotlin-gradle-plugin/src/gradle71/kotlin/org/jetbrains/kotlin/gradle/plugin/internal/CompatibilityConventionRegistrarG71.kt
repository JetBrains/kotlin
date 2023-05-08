/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.internal.HasConvention

internal class CompatibilityConventionRegistrarG71 : CompatibilityConventionRegistrar {
    override fun addConvention(subject: Any, name: String, plugin: Any) {
        (subject as HasConvention).convention.plugins[name] = plugin
    }

    internal class Factory : CompatibilityConventionRegistrar.Factory {
        override fun getInstance(): CompatibilityConventionRegistrar = CompatibilityConventionRegistrarG71()
    }
}