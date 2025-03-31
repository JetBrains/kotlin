/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer

/**
 * Represents a Kotlin DSL entity containing a collection of multiple [Kotlin targets][KotlinTarget] in this project.
 */
@KotlinTargetsDsl
interface KotlinTargetsContainer {

    /**
     * A [NamedDomainObjectContainer] containing all registered [Kotlin targets][KotlinTarget] in this project.
     */
    val targets: NamedDomainObjectCollection<KotlinTarget>
}

/**
 * Represents a Kotlin DSL entity containing a collection of multiple [KotlinSourceSets][KotlinSourceSet].
 */
interface KotlinSourceSetContainer {

    /**
     * A [NamedDomainObjectContainer] containing all registered [KotlinSourceSets][KotlinSourceSet] in this project.
     */
    val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
}
