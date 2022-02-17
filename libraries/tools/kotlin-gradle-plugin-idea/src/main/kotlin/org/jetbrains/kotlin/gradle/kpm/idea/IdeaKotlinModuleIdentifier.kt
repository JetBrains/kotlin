/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinModuleIdentifier : Serializable {
    val moduleClassifier: String?
}

interface IdeaKotlinLocalModuleIdentifier : IdeaKotlinModuleIdentifier {
    val buildId: String
    val projectId: String
}

interface IdeaKotlinMavenModuleIdentifier : IdeaKotlinModuleIdentifier {
    val group: String
    val name: String
}

fun IdeaKotlinModuleIdentifier.deepCopy(interner: Interner = Interner.default()): IdeaKotlinModuleIdentifier {
    return when (this) {
        is IdeaKotlinLocalModuleIdentifier -> this.deepCopy(interner)
        is IdeaKotlinMavenModuleIdentifier -> this.deepCopy(interner)
        else -> throw IllegalArgumentException("Unexpected ${IdeaKotlinMavenModuleIdentifier::class.java.simpleName}: $this")
    }
}

fun IdeaKotlinLocalModuleIdentifier.deepCopy(interner: Interner = Interner.default()): IdeaKotlinLocalModuleIdentifier {
    return IdeaKotlinLocalModuleIdentifierImpl(
        moduleClassifier = interner.intern(moduleClassifier),
        buildId = interner.intern(buildId),
        projectId = interner.intern(projectId)
    )
}

fun IdeaKotlinMavenModuleIdentifier.deepCopy(interner: Interner = Interner.default()): IdeaKotlinMavenModuleIdentifier {
    return IdeaKotlinMavenModuleIdentifierImpl(
        moduleClassifier = interner.intern(moduleClassifier),
        group = interner.intern(group),
        name = interner.intern(name)
    )
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinLocalModuleIdentifierImpl(
    override val moduleClassifier: String?,
    override val buildId: String,
    override val projectId: String
) : IdeaKotlinLocalModuleIdentifier {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinMavenModuleIdentifierImpl(
    override val moduleClassifier: String?,
    override val group: String,
    override val name: String
) : IdeaKotlinMavenModuleIdentifier {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
