/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinPublishing
import javax.inject.Inject

internal abstract class KotlinMultiplatformPublishing @Inject constructor(
    softwareComponentFactory: SoftwareComponentFactory,
    project: Project,
) : KotlinPublishing {
    override val adhocSoftwareComponent: AdhocComponentWithVariants by lazy {
        softwareComponentFactory
            .adhoc("adhocKotlin")
            .also { project.components.add(it) }
    }
}