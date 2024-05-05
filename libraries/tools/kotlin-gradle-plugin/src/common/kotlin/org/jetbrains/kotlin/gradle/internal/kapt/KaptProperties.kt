/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService.*
import org.jetbrains.kotlin.gradle.internal.properties.propertiesService

internal object KaptProperties {
    private val KAPT_VERBOSE = BooleanGradleProperty("kapt.verbose", false)
    private val KAPT_INCREMENTAL_APT = BooleanGradleProperty(
        "kapt.incremental.apt",
        // Currently doesn't match the default value of KaptFlag.INCREMENTAL_APT,
        // but it's fine (see https://github.com/JetBrains/kotlin/pull/3942#discussion_r532578690).
        true
    )
    private val KAPT_INFO_AS_WARNINGS = BooleanGradleProperty("kapt.info.as.warnings", false)
    private val KAPT_INCLUDE_COMPILE_CLASSPATH = BooleanGradleProperty("kapt.include.compile.classpath", true)
    private val KAPT_KEEP_KDOC_COMMENTS_IN_STUBS = BooleanGradleProperty("kapt.keep.kdoc.comments.in.stubs", true)
    private val KAPT_USE_K2 = BooleanGradleProperty("kapt.use.k2", false)
    private val KAPT_DONT_WARN_ANNOTATION_PROCESSOR_DEPENDENCIES = BooleanGradleProperty(
        "kapt.dont.warn.annotationProcessor.dependencies",
        false
    )
    private val CLASSLOADERS_CACHE_DISABLE_FOR_PROCESSORS = StringGradleProperty("kapt.classloaders.cache.disableForProcessors", "")
    private val CLASSLOADERS_CACHE_SIZE = IntGradleProperty("kapt.classloaders.cache.size", 0)

    fun isKaptVerbose(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_VERBOSE, project)
    }

    fun isIncrementalKapt(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_INCREMENTAL_APT, project)
    }

    fun isInfoAsWarnings(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_INFO_AS_WARNINGS, project)
    }

    fun isIncludeCompileClasspath(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_INCLUDE_COMPILE_CLASSPATH, project)
    }

    fun isKaptKeepKdocCommentsInStubs(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_KEEP_KDOC_COMMENTS_IN_STUBS, project)
    }

    fun isUseK2(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_USE_K2, project)
    }

    fun isKaptDontWarnAnnotationProcessorDependencies(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(KAPT_DONT_WARN_ANNOTATION_PROCESSOR_DEPENDENCIES, project)
    }

    fun getClassloadersCacheDisableForProcessors(project: Project): Provider<String> = project.propertiesService.flatMap {
        it.property(CLASSLOADERS_CACHE_DISABLE_FOR_PROCESSORS, project)
    }

    fun getClassloadersCacheSize(project: Project): Provider<Int> = project.propertiesService.flatMap {
        it.property(CLASSLOADERS_CACHE_SIZE, project)
    }
}