/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.internal.AbstractPolymorphicDomainObjectContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.reflect.Instantiator
import org.gradle.model.internal.core.NamedEntityInstantiator
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

class KotlinTargetContainer internal constructor(
    instantiator: Instantiator,
    protected val fileResolver: FileResolver,
    protected val project: Project
) : AbstractPolymorphicDomainObjectContainer<KotlinTarget>(KotlinTarget::class.java, instantiator, { it.targetName }) {
    override fun <U : KotlinTarget?> doCreate(p0: String?, p1: Class<U>?): U {
        TODO("not implemented")
    }

    override fun getCreateableTypes(): MutableSet<out Class<out KotlinTarget>> {
        TODO("not implemented")
    }

    override fun getEntityInstantiator(): NamedEntityInstantiator<KotlinTarget> {
        TODO("not implemented")
    }

    override fun doCreate(p0: String?): KotlinTarget {
        TODO("not implemented")
    }

}