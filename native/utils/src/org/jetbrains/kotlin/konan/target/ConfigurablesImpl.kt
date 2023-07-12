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

class GccConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?)
    : GccConfigurables, KonanPropertiesLoader(target, properties, dependenciesRoot), ConfigurablesWithEmulator {
    override val dependencies: List<String>
        get() = super.dependencies + listOfNotNull(emulatorDependency)
    }

class AndroidConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?)
    : AndroidConfigurables, KonanPropertiesLoader(target, properties, dependenciesRoot)

class WasmConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?)
    : WasmConfigurables, KonanPropertiesLoader(target, properties, dependenciesRoot)

class ZephyrConfigurablesImpl(target: KonanTarget, properties: Properties, dependenciesRoot: String?)
    : ZephyrConfigurables, KonanPropertiesLoader(target, properties, dependenciesRoot)


fun loadConfigurables(target: KonanTarget, properties: Properties, dependenciesRoot: String?): Configurables = when (target.family) {
    Family.LINUX -> GccConfigurablesImpl(target, properties, dependenciesRoot)

    Family.TVOS, Family.WATCHOS, Family.IOS, Family.OSX -> AppleConfigurablesImpl(target, properties, dependenciesRoot)

    Family.ANDROID -> AndroidConfigurablesImpl(target, properties, dependenciesRoot)

    Family.MINGW -> MingwConfigurablesImpl(target, properties, dependenciesRoot)

    Family.WASM -> WasmConfigurablesImpl(target, properties, dependenciesRoot)

    Family.ZEPHYR -> ZephyrConfigurablesImpl(target, properties, dependenciesRoot)
}

