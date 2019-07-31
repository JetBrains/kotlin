/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import groovy.lang.Closure
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution
import org.jetbrains.kotlin.gradle.plugin.KotlinExecution.ExecutionSource
import org.jetbrains.kotlin.gradle.plugin.KotlinTestRun
import org.jetbrains.kotlin.gradle.testing.KotlinAggregatingTestRun
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport


