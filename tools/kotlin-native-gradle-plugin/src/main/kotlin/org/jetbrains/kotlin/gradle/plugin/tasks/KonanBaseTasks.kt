/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.KonanTarget.*

abstract class KonanTargetableTask: DefaultTask() {

    @Optional @Input var target          : String? = null
        internal set

    internal val targetManager: TargetManager by lazy {
        TargetManager(target ?: "host")
    } @Internal get


    val targetIsSupported: Boolean
        @Internal get() = targetManager.target.enabled

    val isCrossCompile: Boolean
        @Internal get() = (targetManager.target != TargetManager.host)

    @Internal fun produceSuffix(produce: String): String
        = CompilerOutputKind.valueOf(produce.toUpperCase())
            .suffix(targetManager.target)
}

internal val Project.host
    get() = TargetManager.host.name.toLowerCase()

internal val Project.simpleOsName
    get() = TargetManager.simpleOsName()
