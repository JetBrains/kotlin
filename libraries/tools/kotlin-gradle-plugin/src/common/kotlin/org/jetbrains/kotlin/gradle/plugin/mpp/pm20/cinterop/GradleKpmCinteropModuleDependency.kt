/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModelContainer
import org.jetbrains.kotlin.project.model.utils.findRefiningFragments
import java.io.File

internal fun GradleKpmFragment.declareCinteropDependency(cinteropName: String) {
    val cinteropModule = project.kpmModelContainer.cinteropModules.maybeCreate(cinteropName)
    cinteropModule.applyFragmentRequirements(this)

    val allRequestingFragments = containingModule.findRefiningFragments(this) + this
    allRequestingFragments.forEach {
        (it as GradleKpmFragment).addCinteropDependencyToApiConfiguration(cinteropModule)
    }
}

private fun GradleKpmFragment.addCinteropDependencyToApiConfiguration(
    cinteropModule: GradleKpmCinteropModule
) {
    val corresponding = cinteropModule.fragments.first { it.fragmentName == fragmentName }
    project.dependencies.add(apiConfigurationName, CinteropDependency(corresponding))
}

private class CinteropDependency(
    private val fragment: GradleKpmCinteropFragment
) : AbstractDependency(), SelfResolvingDependencyInternal, FileCollectionDependency {
    override fun getGroup(): String = fragment.containingModule.project.group.toString()
    override fun getName(): String = fragment.containingModule.name + "-cinterop-" + fragment.fragmentName
    override fun getVersion(): String = fragment.containingModule.project.version.toString()

    override fun copy(): Dependency = CinteropDependency(fragment)

    override fun getBuildDependencies(): TaskDependency = fragment.outputFiles.get().buildDependencies

    override fun resolve(): MutableSet<File> = fragment.outputFiles.get().files

    override fun resolve(transitive: Boolean): MutableSet<File> = resolve()

    override fun getFiles(): FileCollection = fragment.outputFiles.get()

    override fun getTargetComponentId(): ComponentIdentifier? = null

    override fun contentEquals(dependency: Dependency): Boolean {
        if (dependency !is CinteropDependency) return false
        return dependency.fragment == fragment
    }
}
