/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.java

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaConfigurationBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.BaseTestDokkaSourceSetBuilder
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaSourceSet
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * An implementation of [BaseTestDokkaConfigurationBuilder] specific to [JavaTestProject].
 */
class JavaTestConfigurationBuilder : BaseTestDokkaConfigurationBuilder() {
    override var moduleName: String = "javaTestProject"

    private val javaSourceSetBuilder = JavaTestSourceSetBuilder()

    @AnalysisTestDslMarker
    fun javaSourceSet(fillSourceSet: JavaTestSourceSetBuilder.() -> Unit) {
        fillSourceSet(javaSourceSetBuilder)
    }

    override fun verify() {
        super.verify()
        javaSourceSetBuilder.verify()
    }

    override fun build(): TestDokkaConfiguration {
        return TestDokkaConfiguration(
            moduleName = moduleName,
            includes = includes,
            sourceSets = setOf(javaSourceSetBuilder.build())
        )
    }
}

/**
 * An implementation of [BaseTestDokkaSourceSetBuilder] specific to [JavaTestProject].
 *
 * Defines sensible defaults that should cover the majority of simple projects.
 */
class JavaTestSourceSetBuilder : BaseTestDokkaSourceSetBuilder() {
    override fun build(): TestDokkaSourceSet {
        return TestDokkaSourceSet(
            analysisPlatform = Platform.jvm,
            displayName = "JavaJvmSourceSet",
            sourceSetID = JavaTestProject.DEFAULT_SOURCE_SET_ID,
            dependentSourceSets = setOf(),
            sourceRoots = additionalSourceRoots + setOf(JavaTestProject.DEFAULT_SOURCE_ROOT),
            classpath = additionalClasspath, // TODO [beresnev] is kotlin jvm stdlib needed here?
            includes = includes,
            samples = samples,
            languageVersion = languageVersion,
            apiVersion = apiVersion
        )
    }
}
