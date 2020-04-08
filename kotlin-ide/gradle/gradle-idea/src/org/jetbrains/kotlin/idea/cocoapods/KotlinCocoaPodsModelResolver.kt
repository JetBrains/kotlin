/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.cocoapods

import org.jetbrains.kotlin.gradle.EnablePodImportTask
import org.jetbrains.plugins.gradle.model.ClassSetProjectImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinCocoaPodsModelResolver : AbstractProjectResolverExtension() {

    override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider {
        return ClassSetProjectImportModelProvider(
            setOf(EnablePodImportTask::class.java)
        )
    }

    override fun requiresTaskRunning(): Boolean = true
}


