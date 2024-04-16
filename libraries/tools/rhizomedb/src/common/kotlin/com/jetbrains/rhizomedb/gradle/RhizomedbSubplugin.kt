/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.rhizomedb.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class RhizomedbGradleSubplugin :
    KotlinCompilerPluginSupportPlugin {

    companion object {
        const val RHIZOMEDB_GROUP_NAME = "org.jetbrains.kotlin"
        const val RHIZOMEDB_ARTIFACT_NAME = "rhizomedb-compiler-plugin.embeddable"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(RHIZOMEDB_GROUP_NAME, RHIZOMEDB_ARTIFACT_NAME)

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.rhizomedb"
}
