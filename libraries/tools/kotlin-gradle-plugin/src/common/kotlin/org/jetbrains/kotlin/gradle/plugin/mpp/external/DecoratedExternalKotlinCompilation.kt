/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

/**
 * Similar to [DecoratedExternalKotlinTarget]:
 * This class is intended to be decorated: Enabling external target authors to give compilations a custom type.
 * The implementation of [KotlinCompilation] is handled by a backed, internal, opaque instance provided by the [Delegate]
 *
 * #### Create an instance
 * Creating an instance will require an instance of [Delegate] which can only be retrieved by calling into the
 * [DecoratedExternalKotlinTarget.createCompilation] functions and providing a [ExternalKotlinCompilationDescriptor.compilationFactory]
 */
@ExternalKotlinTargetApi
abstract class DecoratedExternalKotlinCompilation(delegate: Delegate) :
    DecoratedKotlinCompilation<KotlinCommonOptions>(delegate.compilation) {
    open class Delegate internal constructor(internal open val compilation: KotlinCompilationImpl)
}

