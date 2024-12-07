/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test

class KT58838LazyCinteropConfiguration {
    @Test
    fun `test - configure cinterop settings lazy`() = buildProjectWithMPP().runLifecycleAwareTest {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        /**
         * Regression:
         * java.lang.IllegalStateException: Could not create domain object 'foo' (DefaultCInteropSettings)
         * 	at org.gradle.api.internal.DefaultNamedDomainObjectCollection$AbstractDomainObjectCreatingProvider.domainObjectCreationException(DefaultNamedDomainObjectCollection.java:976)
         * 	at org.gradle.api.internal.DefaultNamedDomainObjectCollection$AbstractDomainObjectCreatingProvider.tryCreate(DefaultNamedDomainObjectCollection.java:948)
         * 	at org.gradle.api.internal.DefaultNamedDomainObjectCollection$AbstractDomainObjectCreatingProvider.calculateOwnValue(DefaultNamedDomainObjectCollection.java:929)
         * 	at org.gradle.api.internal.DefaultDomainObjectCollection.addLater(DefaultDomainObjectCollection.java:286)
         * 	at org.gradle.api.internal.DefaultNamedDomainObjectCollection.addLater(DefaultNamedDomainObjectCollection.java:146)
         * 	at org.gradle.api.internal.AbstractNamedDomainObjectContainer.createDomainObjectProvider(AbstractNamedDomainObjectContainer.java:122)
         * 	at org.gradle.api.internal.AbstractNamedDomainObjectContainer.register(AbstractNamedDomainObjectContainer.java:108)
         * 	at org.jetbrains.kotlin.gradle.dependencyResolutionTests.KT58838LazyCinteropConfiguration$test - configure cinterop settings lazy$1$1.execute(KT58838LazyCinteropConfiguration.kt:22)
         * 	at org.jetbrains.kotlin.gradle.dependencyResolutionTests.KT58838LazyCinteropConfiguration$test - configure cinterop settings lazy$1$1.execute(KT58838LazyCinteropConfiguration.kt:21)
         * 	...

         * Caused by: org.gradle.api.internal.AbstractMutationGuard$IllegalMutationException: Project#afterEvaluate(Action) on root project 'test' cannot be executed in the current context.
         * 	at org.gradle.api.internal.AbstractMutationGuard.createIllegalStateException(AbstractMutationGuard.java:39)
         * 	at org.gradle.api.internal.AbstractMutationGuard.assertMutationAllowed(AbstractMutationGuard.java:34)
         * 	at org.gradle.api.internal.project.DefaultProject.assertMutatingMethodAllowed(DefaultProject.java:1449)
         * 	at org.gradle.api.internal.project.DefaultProject.afterEvaluate(DefaultProject.java:1053)
         * 	at org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginKt.whenEvaluated(KotlinMultiplatformPlugin.kt:236)
         * 	at org.jetbrains.kotlin.gradle.targets.native.internal.CInteropConfigurationsKt.locateOrCreateCInteropDependencyConfiguration(CInteropConfigurations.kt:54)
         * 	... 70 more
         */
        kotlin.linuxX64().compilations.named("main") {
            it.cinterops.register("foo")
        }
    }
}