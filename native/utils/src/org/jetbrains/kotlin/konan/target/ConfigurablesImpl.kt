/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.util.ProgressCallback

class GccConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?, progressCallback: ProgressCallback) : GccConfigurables,
    KonanPropertiesLoader(target, properties, dependenciesRoot, progressCallback = progressCallback), ConfigurablesWithEmulator {
    override val dependencies: List<String>
        get() = super.dependencies + listOfNotNull(emulatorDependency)
}

class AndroidConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?, progressCallback: ProgressCallback) : AndroidConfigurables,
    KonanPropertiesLoader(target, properties, dependenciesRoot, progressCallback = progressCallback)

fun loadConfigurables(
    target: KonanTarget,
    properties: Properties,
    dependenciesRoot: String?,
    progressCallback: ProgressCallback = { url, currentBytes, totalBytes ->
        print("\n(KonanProperties) Downloading dependency: $url (${currentBytes}/${totalBytes}). ")
    },
): Configurables = when (target.family) {
    Family.LINUX -> GccConfigurablesImpl(target, properties, dependenciesRoot, progressCallback)

    Family.TVOS, Family.WATCHOS, Family.IOS, Family.OSX -> AppleConfigurablesImpl(target, properties, dependenciesRoot, progressCallback)

    Family.ANDROID -> AndroidConfigurablesImpl(target, properties, dependenciesRoot, progressCallback)

    Family.MINGW -> MingwConfigurablesImpl(target, properties, dependenciesRoot, progressCallback)
}
