/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget

data class PrototypeAndroidDsl(
    var compileSdk: Int
)

class PrototypeAndroidTarget(
    delegate: Delegate,
    val androidDsl: PrototypeAndroidDsl
) : DecoratedExternalKotlinTarget(delegate) {
    internal val kotlin = super.project.extensions.getByType<KotlinMultiplatformExtension>()

    @Suppress("unchecked_cast")
    override val compilations: NamedDomainObjectContainer<PrototypeAndroidCompilation>
        get() = super.compilations as NamedDomainObjectContainer<PrototypeAndroidCompilation>
}