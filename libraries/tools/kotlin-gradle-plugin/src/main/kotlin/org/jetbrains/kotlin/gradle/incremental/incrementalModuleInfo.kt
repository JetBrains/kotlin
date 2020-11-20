/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo

/**
 * Provider of [IncrementalModuleInfo] that allows concrete implementation to e.g use Gradle build services
 * or rely on some static stats.
 */
interface IncrementalModuleInfoProvider {
    val info: IncrementalModuleInfo
}

/** A build service used to provide [IncrementalModuleInfo] instance for all tasks. */
abstract class IncrementalModuleInfoBuildService : BuildService<IncrementalModuleInfoBuildService.Parameters>,
    IncrementalModuleInfoProvider {
    abstract class Parameters : BuildServiceParameters {
        abstract val info: Property<IncrementalModuleInfo>
    }

    override val info: IncrementalModuleInfo
        get() = parameters.info.get()

    companion object {
        // Use class name + class loader in case there are multiple class loaders in the same build
        fun getServiceName(): String {
            val clazz = IncrementalModuleInfoBuildService::class.java
            return clazz.canonicalName + "_" + clazz.classLoader.hashCode()
        }
    }
}
