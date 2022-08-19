/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetHierarchyDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor

internal class KotlinTargetHierarchyDslImpl(
    private val targets: DomainObjectCollection<KotlinTarget>,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinTargetHierarchyDsl {
    override fun apply(
        hierarchyDescriptor: KotlinTargetHierarchyDescriptor,
        describeExtension: (KotlinTargetHierarchyBuilder.() -> Unit)?
    ) {
        applyKotlinTargetHierarchy(hierarchyDescriptor.extendIfNotNull(describeExtension), targets, sourceSets)
    }

    override fun default(describeExtension: (KotlinTargetHierarchyBuilder.() -> Unit)?) {
        apply(naturalKotlinTargetHierarchy, describeExtension)
    }

    override fun custom(describe: KotlinTargetHierarchyBuilder.() -> Unit) {
        apply(KotlinTargetHierarchyDescriptor(describe))
    }
}

private fun KotlinTargetHierarchyDescriptor.extendIfNotNull(describe: (KotlinTargetHierarchyBuilder.() -> Unit)?) =
    if (describe == null) this else extend(describe)