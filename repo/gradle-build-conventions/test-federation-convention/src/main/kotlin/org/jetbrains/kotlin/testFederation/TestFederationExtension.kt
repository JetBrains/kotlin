/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

open class TestFederationExtension @Inject constructor(project: Project) {
    /**
     * The module 'repo:test-federation-runtime' is added to test's as a default dependency.
     * This property can be used to disable this default.
     */
    val defaultDependencyEnabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
}
