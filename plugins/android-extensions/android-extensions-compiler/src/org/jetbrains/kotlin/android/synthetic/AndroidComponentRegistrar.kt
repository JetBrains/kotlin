/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("MayBeConstant", "unused")

package org.jetbrains.kotlin.android.synthetic

import org.jetbrains.kotlin.android.synthetic.diagnostic.AndroidExtensionPropertiesCallChecker
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm

class AndroidCommandLineProcessor {
    companion object {
        val ANDROID_COMPILER_PLUGIN_ID: String = "org.jetbrains.kotlin.android"
        val EXPERIMENTAL_OPTION: CliOption =
            CliOption("experimental", "true/false", "Enable experimental features", required = false)
        val DEFAULT_CACHE_IMPL_OPTION: CliOption =
            CliOption("defaultCacheImplementation", "hashMap/sparseArray/none", "Default cache implementation for module", required = false)
        val ENABLED_OPTION: CliOption =
            CliOption("enabled", "true/false", "Enable Android Extensions", required = false)
    }
}

class AndroidExtensionPropertiesComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
    ) {
        if (platform.isJvm()) {
            container.useInstance(AndroidExtensionPropertiesCallChecker())
        }
    }
}
