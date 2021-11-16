/*
* Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.external

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget

fun KotlinMultiplatformExtension.externalTarget(
    targetName: String,
    platformType: KotlinPlatformType,
): KotlinExternalTargetHandle {
    val target = KotlinExternalTarget(targetName, platformType, project)
    targets.add(target)
    return KotlinExternalTargetHandle(target)
}

internal class KotlinExternalTarget(
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    project: Project
) : AbstractKotlinTarget(project) {

    internal val compilationsFactory = KotlinJvmExternalCompilationFactory(project, this)

    override val kotlinComponents: Set<KotlinTargetComponent>
        get() = emptySet()  // TODO NOW

    override val components: Set<SoftwareComponent>
        get() = emptySet()  // TODO NOW

    override val compilations: NamedDomainObjectContainer<KotlinJvmExternalCompilation> =
        project.container(KotlinJvmExternalCompilation::class.java, compilationsFactory)

}


