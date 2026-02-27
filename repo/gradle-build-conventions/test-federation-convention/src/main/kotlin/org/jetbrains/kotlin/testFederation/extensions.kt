/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * The test federation is typically only enabled in CI environments.
 * Running tests locally will always execute all tests unless actively opted into the test federation.
 */
internal val Project.testFederationEnabled: Provider<Boolean>
    get() = providers.gradleProperty(TEST_FEDERATION_ENABLED_KEY).map { it.toBoolean() }
        .orElse(providers.environmentVariable(TEST_FEDERATION_ENABLED_ENV_KEY).map { it.toBoolean() })


/**
 * Returns the [Subsystem] the current project belongs to
 */
internal val Project.testFederationSubsystem: Subsystem
    get() {
        val projectDir = repositoryPath(this.projectDir.toPath())
        val buildFile = repositoryPath(this.buildFile.toPath())
        return SubsystemInfo.all.lastOrNull { info -> projectDir in info || buildFile in info }?.system ?: Subsystem.Unknown
    }

internal val Project.testFederationMode: Provider<TestFederationMode>
    get() {
        val mySubsystem = project.testFederationSubsystem
        return project.testFederationAffectedSubsystems.map { affectedTestSystems ->
            if (mySubsystem in affectedTestSystems) TestFederationMode.Full
            else TestFederationMode.Smoke
        }
    }

internal val Project.testFederationAffectedSubsystems: Provider<Set<Subsystem>>
    get() {
        return providers.environmentVariable(TEST_FEDERATION_AFFECTED_SUBSYSTEMS_ENV_KEY)
            .map { it.split(";").map { Subsystem.valueOf(it) }.toSet() }
            .orElse(project.affectedSubsystemsService.map { it.affectedSubsystems })
    }
