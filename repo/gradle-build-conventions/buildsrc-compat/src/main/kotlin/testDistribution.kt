/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.gradle.develocity.agent.gradle.test.TestDistributionConfiguration
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.develocity


fun Test.configureTestDistribution(configure: TestDistributionConfiguration.() -> Unit = {}) {
    val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild
    val testDistributionEnabled =
        project.findProperty("kotlin.build.test.distribution.enabled")?.toString()?.toBoolean() ?: false

    useJUnitPlatform()
    develocity.testDistribution {
        enabled.set(testDistributionEnabled)
        maxRemoteExecutors.set(20)
        if (isTeamcityBuild) {
            requirements.set(setOf("os=${OperatingSystem.current().familyName}"))
        } else {
            maxLocalExecutors.set(0)
        }
        configure()
    }
}
