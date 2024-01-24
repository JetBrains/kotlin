/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.mixed

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaSourceSetBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaSourceSet
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.getKotlinJvmStdlibJarPath
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * An implementation of [BaseTestDokkaConfigurationBuilder] specific to [MixedJvmTestProject].
 */
class MixedJvmTestConfigurationBuilder : BaseTestDokkaConfigurationBuilder() {
    override var moduleName: String = "mixedJvmTestProject"

    private val mixedJvmSourceSetBuilder = MixedJvmTestSourceSetBuilder()

    @AnalysisTestDslMarker
    fun jvmSourceSet(fillSourceDir: MixedJvmTestSourceSetBuilder.() -> Unit) {
        fillSourceDir(mixedJvmSourceSetBuilder)
    }

    override fun verify() {
        super.verify()
        mixedJvmSourceSetBuilder.verify()
    }

    override fun build(): TestDokkaConfiguration {
        return TestDokkaConfiguration(
            moduleName = moduleName,
            includes = includes,
            sourceSets = setOf(mixedJvmSourceSetBuilder.build())
        )
    }

    override fun toString(): String {
        return "MixedJvmTestConfigurationBuilder(" +
                "moduleName='$moduleName', " +
                "jvmSourceSetBuilder=$mixedJvmSourceSetBuilder" +
                ")"
    }
}

class MixedJvmTestSourceSetBuilder : BaseTestDokkaSourceSetBuilder() {
    override fun build(): TestDokkaSourceSet {
        return TestDokkaSourceSet(
            analysisPlatform = Platform.jvm,
            displayName = "MixedJvmSourceSet",
            sourceSetID = DokkaSourceSetID(scopeId = "project", sourceSetName = "jvm"),
            dependentSourceSets = setOf(),
            sourceRoots = additionalSourceRoots + setOf(
                KotlinJvmTestProject.DEFAULT_SOURCE_ROOT,
                JavaTestProject.DEFAULT_SOURCE_ROOT
            ),
            classpath = additionalClasspath + setOf(getKotlinJvmStdlibJarPath()),
            includes = includes,
            samples = samples,
            languageVersion = languageVersion,
            apiVersion = apiVersion
        )
    }
}
