/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.kotlin

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaSourceSetBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaSourceSet
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * An implementation of [BaseTestDokkaConfigurationBuilder] specific to [KotlinJvmTestProject].
 */
class KotlinJvmTestConfigurationBuilder : BaseTestDokkaConfigurationBuilder() {
    override var moduleName: String = "kotlinJvmTestProject"

    private val kotlinSourceSetBuilder = KotlinJvmTestSourceSetBuilder()

    @AnalysisTestDslMarker
    fun kotlinSourceSet(fillSourceSet: KotlinJvmTestSourceSetBuilder.() -> Unit) {
        fillSourceSet(kotlinSourceSetBuilder)
    }

    override fun verify() {
        super.verify()
        kotlinSourceSetBuilder.verify()
    }

    override fun build(): TestDokkaConfiguration {
        return TestDokkaConfiguration(
            moduleName = moduleName,
            includes = includes,
            sourceSets = setOf(kotlinSourceSetBuilder.build())
        )
    }
}

class KotlinJvmTestSourceSetBuilder : BaseTestDokkaSourceSetBuilder() {
    override fun build(): TestDokkaSourceSet {
        return TestDokkaSourceSet(
            analysisPlatform = Platform.jvm,
            displayName = "KotlinJvmSourceSet",
            sourceSetID = KotlinJvmTestProject.DEFAULT_SOURCE_SET_ID,
            dependentSourceSets = setOf(),
            sourceRoots = additionalSourceRoots + setOf(KotlinJvmTestProject.DEFAULT_SOURCE_ROOT),
            classpath = additionalClasspath + setOf(getKotlinJvmStdlibJarPath()),
            includes = includes,
            samples = samples,
            languageVersion = languageVersion,
            apiVersion = apiVersion
        )
    }
}
