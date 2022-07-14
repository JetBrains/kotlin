/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnusedReceiverParameter") // receivers are convenient for DSL scoping

package org.jetbrains.kotlin.project.model.testDsl

import org.jetbrains.kotlin.project.model.KpmLocalModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmMavenModuleIdentifier
import org.jetbrains.kotlin.project.model.infra.KpmTestEntity

fun KpmTestEntity.project(projectId: String): KpmLocalModuleIdentifier = KpmLocalModuleIdentifier("", projectId, null)

val KpmLocalModuleIdentifier.test: KpmLocalModuleIdentifier
    get() = KpmLocalModuleIdentifier(buildId, projectId, "test")

// TODO: custom aux modules

// TODO: scopes
fun KpmTestEntity.maven(group: String, name: String): KpmMavenModuleIdentifier = KpmMavenModuleIdentifier(group, name, null)

// TODO: published aux modules
