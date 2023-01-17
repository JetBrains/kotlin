/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec

val Gradle.projectCacheDir
    get() = startParameter.projectCacheDir ?: this.rootProject.projectDir.resolve(".gradle")

internal inline fun <reified S : BuildService<*>> fullServiceName(): String {
    val klass = S::class.java
    return "${klass.canonicalName}-${klass.classLoader.hashCode()}"
}

internal inline fun <reified P : BuildServiceParameters, reified S : BuildService<P>> Gradle.registerSharedService(
    crossinline fn: BuildServiceSpec<P>.() -> Unit = {}
): Provider<S> =
    sharedServices.registerIfAbsent(fullServiceName<S>(), S::class.java) {
        fn(it)
    }