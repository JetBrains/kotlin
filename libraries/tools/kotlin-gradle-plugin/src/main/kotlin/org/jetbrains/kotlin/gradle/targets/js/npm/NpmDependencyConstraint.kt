/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.artifacts.ModuleVersionSelectorStrictSpec
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.internal.artifacts.dependencies.DependencyConstraintInternal
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

class NpmDependencyConstraint(
    internal val project: Project,
    private val path: String,
    private val version: String
) : DependencyConstraintInternal {

    private var reason: String? = null

    private val versionConstraint = NpmVersionConstraint(version)

    override fun getGroup(): String? = null

    override fun getName(): String = path

    override fun getVersion(): String = versionConstraint.version

    override fun matchesStrictly(identifier: ModuleVersionIdentifier): Boolean {
        return ModuleVersionSelectorStrictSpec(this)
            .isSatisfiedBy(identifier)
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

    override fun attributes(configureAction: Action<in AttributeContainer>): DependencyConstraint {
        warnAboutInternalApiUse()
        return this
    }

    private fun warnAboutInternalApiUse() {
        project.logger.warn(
            """Cannot set attributes for NPM constraint '${this.path}:${this.version}"""
        )
    }

    override fun version(configureAction: Action<in MutableVersionConstraint>) {
        configureAction.execute(versionConstraint)
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
        nodeJs.npmResolutionManager.putNpmResolution(path, versionConstraint.toSemVer())
    }

    override fun getReason(): String? {
        return reason
    }

    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun getVersionConstraint(): VersionConstraint {
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
        nodeJs.npmResolutionManager.putNpmResolution(path, versionConstraint.toSemVer())
        return versionConstraint
    }

    override fun setForce(force: Boolean) {
    }

    override fun isForce(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NpmDependencyConstraint

        if (project != other.project) return false
        if (path != other.path) return false
        if (versionConstraint != other.versionConstraint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + versionConstraint.hashCode()
        return result
    }

    override fun toString(): String {
        return "NpmDependencyConstraint(path='$path', versionConstraint=$versionConstraint)"
    }
}