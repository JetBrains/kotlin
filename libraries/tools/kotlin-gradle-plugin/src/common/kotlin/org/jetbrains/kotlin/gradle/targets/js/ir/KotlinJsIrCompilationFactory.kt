/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationFriendPathsResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JsIrCompilationSourceSetsContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JsKotlinCompilationDependencyConfigurationsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinJsCompilerOptionsFactory

class KotlinJsIrCompilationFactory internal constructor(
    override val target: KotlinOnlyTarget<KotlinJsIrCompilation>
) : KotlinCompilationFactory<KotlinJsIrCompilation> {
    override val itemClass: Class<KotlinJsIrCompilation>
        get() = KotlinJsIrCompilation::class.java

    private val compilationImplFactory: KotlinCompilationImplFactory = KotlinCompilationImplFactory(
        compilerOptionsFactory = KotlinJsCompilerOptionsFactory,
        compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver(
            friendArtifactResolver = DefaultKotlinCompilationFriendPathsResolver.FriendArtifactResolver { _ ->
                target.project.files()
            }
        ),
        compilationSourceSetsContainerFactory = JsIrCompilationSourceSetsContainerFactory,
        compilationDependencyConfigurationsFactory = JsKotlinCompilationDependencyConfigurationsFactory
    )

    override fun create(name: String): KotlinJsIrCompilation = target.project.objects.newInstance(
        itemClass, compilationImplFactory.create(target, name)
    )
}
