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

class GccConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : GccConfigurables, KonanPropertiesLoader(target, properties, baseDir), ConfigurablesWithEmulator {
    override val dependencies: List<String>
        get() = super.dependencies + listOfNotNull(emulatorDependency)
    }

class AndroidConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : AndroidConfigurables, KonanPropertiesLoader(target, properties, baseDir)

class WasmConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : WasmConfigurables, KonanPropertiesLoader(target, properties, baseDir)

class ZephyrConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : ZephyrConfigurables, KonanPropertiesLoader(target, properties, baseDir)


fun loadConfigurables(target: KonanTarget, properties: Properties, baseDir: String?): Configurables = when (target.family) {
    Family.LINUX -> GccConfigurablesImpl(target, properties, baseDir)

    Family.TVOS, Family.WATCHOS, Family.IOS, Family.OSX -> AppleConfigurablesImpl(target, properties, baseDir)

    Family.ANDROID -> AndroidConfigurablesImpl(target, properties, baseDir)

    Family.MINGW -> MingwConfigurablesImpl(target, properties, baseDir)

    Family.WASM -> WasmConfigurablesImpl(target, properties, baseDir)

    Family.ZEPHYR -> ZephyrConfigurablesImpl(target, properties, baseDir)
}

