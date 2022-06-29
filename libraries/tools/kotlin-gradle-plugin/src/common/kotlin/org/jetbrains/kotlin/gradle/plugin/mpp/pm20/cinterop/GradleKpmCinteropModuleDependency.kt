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
import org.jetbrains.kotlin.project.model.utils.withRefiningFragments
import java.io.File

internal fun GradleKpmFragment.declareCinteropDependency(cinteropName: String) {
    val cinteropModule = project.kpmModelContainer.cinteropModules.maybeCreate(cinteropName)
    val requestingFragment = this

    setupCinteropDependency(cinteropModule, requestingFragment)
    containingModule.fragments.all { fragment ->
        fragment.declaredRefinesDependencies.all { refine ->
            if (refine.withRefinesClosure.contains(requestingFragment)) {
                setupCinteropDependency(cinteropModule, fragment)
            }
        }
    }
}

private fun setupCinteropDependency(cinteropModule: GradleKpmCinteropModule, requestingFragment: GradleKpmFragment) {
    cinteropModule.applyFragmentRequirements(requestingFragment)
    requestingFragment.withRefiningFragments().forEach {
        val fragment = it as GradleKpmFragment
        val corresponding = cinteropModule.getFragmentByName(fragment.fragmentName)!!
        fragment.project.dependencies.add(fragment.cinteropConfiguration.name, CinteropDependency(corresponding))
    }
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
