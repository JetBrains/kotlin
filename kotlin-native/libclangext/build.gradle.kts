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

import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.*

plugins {
    id("kotlin.native.build-tools-conventions")
    id("native")
}
val libclangextEnabled = org.jetbrains.kotlin.konan.target.HostManager.hostIsMac
extra["isEnabled"] = libclangextEnabled

native {
    val isWindows = PlatformInfo.isWindows()
    val obj = if (isWindows) "obj" else "o"
    val cxxflags = mutableListOf("--std=c++17", "-g",
                          "-Isrc/main/include",
                          "-I$llvmDir/include",
                          "-DLLVM_DISABLE_ABI_BREAKING_CHECKS_ENFORCING=1")
    if (libclangextEnabled) {
        cxxflags += "-DLIBCLANGEXT_ENABLE=1"
    }
    suffixes {
        (".cpp" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
            flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            dir("src/main/cpp")
        }
    }
    val objSet = sourceSets["main"]!!.transform(".cpp" to ".$obj")
    target(lib("clangext"), objSet) {
        tool(*hostPlatform.clangForJni.llvmAr("").toTypedArray())
        flags("-qcv", ruleOut(), *ruleInAll())
    }
}