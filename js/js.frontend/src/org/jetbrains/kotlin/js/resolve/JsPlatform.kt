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

package org.jetbrains.kotlin.js.resolve

import com.google.common.collect.ImmutableList
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform

object JsPlatform : TargetPlatform("JS") {
    override val defaultModuleParameters = object : ModuleParameters {
        override val defaultImports: List<ImportPath> = ImmutableList.of(
                ImportPath("java.lang.*"),
                ImportPath("kotlin.*"),
                ImportPath("kotlin.annotation.*"),
                ImportPath("kotlin.collections.*"),
                ImportPath("kotlin.ranges.*"),
                ImportPath("kotlin.sequences.*"),
                ImportPath("kotlin.text.*"),
                ImportPath("kotlin.js.*")
        )
    }

    override val platformConfigurator: PlatformConfigurator = JsPlatformConfigurator

    val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance
}