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

class MingwConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : MingwConfigurables, KonanPropertiesLoader(target, properties, baseDir)

class WasmConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : WasmConfigurables, KonanPropertiesLoader(target, properties, baseDir)

class ZephyrConfigurablesImpl(target: KonanTarget, properties: Properties, baseDir: String?)
    : ZephyrConfigurables, KonanPropertiesLoader(target, properties, baseDir)


fun loadConfigurables(target: KonanTarget, properties: Properties, baseDir: String?): Configurables = when (target)  {
        KonanTarget.LINUX_X64, KonanTarget.LINUX_ARM32_HFP, KonanTarget.LINUX_ARM64,
        KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 ->
            GccConfigurablesImpl(target, properties, baseDir)

        KonanTarget.MACOS_X64,
        KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64, KonanTarget.IOS_X64,
        KonanTarget.TVOS_ARM64, KonanTarget.TVOS_X64,
        KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_ARM32,
        KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_X86 ->
            AppleConfigurablesImpl(target, properties, baseDir)

        KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64,
        KonanTarget.ANDROID_X86, KonanTarget.ANDROID_X64 ->
            AndroidConfigurablesImpl(target, properties, baseDir)

        KonanTarget.MINGW_X64, KonanTarget.MINGW_X86 ->
            MingwConfigurablesImpl(target, properties, baseDir)

        KonanTarget.WASM32 ->
            WasmConfigurablesImpl(target, properties, baseDir)

        is KonanTarget.ZEPHYR ->
                ZephyrConfigurablesImpl(target, properties, baseDir)
}

