/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution

class Yarn internal constructor(
    execOps: ExecOperations,
    objects: ObjectFactory,
) : NpmApiExecution<YarnEnvironment> by YarnWorkspaces(
    execOps,
    objects,
)