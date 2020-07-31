/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.internal.artifacts.dependencies.DependencyConstraintInternal
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

class NpmDependencyConstraint(
    internal val project: Project,
    private val path: String,
    private val version: String
) : DependencyConstraintInternal {

    private val versionConstraint = DefaultMutableVersionConstraint(version)

    override fun getGroup(): String? = null

    override fun getName(): String = path

    override fun getVersion(): String = versionConstraint.version

    override fun matchesStrictly(p0: ModuleVersionIdentifier): Boolean {
        TODO("Not yet implemented")
    }

    override fun getModule(): ModuleIdentifier {
        return object : ModuleIdentifier {
            override fun getGroup(): String? = null

            override fun getName(): String = path
        }
    }

    override fun getAttributes(): AttributeContainer {
        return ImmutableAttributes.EMPTY
    }

    override fun attributes(p0: Action<in AttributeContainer>): DependencyConstraint {
        return this
    }

    override fun version(p0: Action<in MutableVersionConstraint>) {
        TODO("Not yet implemented")
    }

    override fun getReason(): String? {
        return null
    }

    override fun because(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getVersionConstraint(): VersionConstraint {
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
        nodeJs.npmResolutionManager.putNpmResolution(path, version)
        return versionConstraint
    }

    override fun setForce(p0: Boolean) {

    }

    override fun isForce(): Boolean {
        return false
    }
}