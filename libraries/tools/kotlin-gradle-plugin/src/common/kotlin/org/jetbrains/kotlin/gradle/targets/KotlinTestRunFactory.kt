/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.NamedDomainObjectFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinTestRun

internal interface KotlinTestRunFactory<T : KotlinTestRun<*>> : NamedDomainObjectFactory<T>