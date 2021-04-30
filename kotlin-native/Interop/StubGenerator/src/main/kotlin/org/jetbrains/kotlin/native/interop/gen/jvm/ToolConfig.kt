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

package  org.jetbrains.kotlin.native.interop.tool

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.Language

class ToolConfig(userProvidedTargetName: String?, flavor: KotlinPlatform) {

    private val konanHome = KonanHomeProvider.determineKonanHome()
    private val distribution = customerDistribution(konanHome)
    private val platformManager = PlatformManager(distribution)
    private val targetManager = platformManager.targetManager(userProvidedTargetName)
    private val host = HostManager.host
    val target = targetManager.target

    private val platform = platformManager.platform(target)

    val clang = platform.clang

    val substitutions = defaultTargetSubstitutions(target)

    fun downloadDependencies() = platform.downloadDependencies()

    fun getDefaultCompilerOptsForLanguage(language: Language): List<String> = when (language) {
        Language.C,
        Language.OBJECTIVE_C -> platform.clang.libclangArgs.toList()
        Language.CPP -> platform.clang.libclangXXArgs.toList()
    }

    val platformCompilerOpts = if (flavor == KotlinPlatform.JVM)
            platform.clang.hostCompilerArgsForJni.toList() else emptyList()

    val llvmHome = platform.absoluteLlvmHome
    val sysRoot = platform.absoluteTargetSysRoot

    val libclang = when (host) {
        KonanTarget.MINGW_X64 -> "$llvmHome/bin/libclang.dll"
        else -> "$llvmHome/lib/${System.mapLibraryName("clang")}"
    }
}
