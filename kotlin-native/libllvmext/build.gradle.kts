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

import org.jetbrains.gradle.plugins.tools.lib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.Family.*
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    `native`
}

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    val llvmDir = project.findProperty("llvmDir")
    val cxxflags = mutableListOf(
        "--std=c++17",
        "-I${llvmDir}/include",
        "-Isrc/main/include"
    )
    when (org.jetbrains.kotlin.konan.target.HostManager.host.family) {
        LINUX -> {
            cxxflags.addAll(listOf("-DKONAN_LINUX=1"))
        }
        MINGW -> {
            cxxflags += "-DKONAN_WINDOWS=1"
        }
        OSX -> {
            cxxflags += "-DKONAN_MACOS=1"
        }
    }
    suffixes {
        (".cpp" to ".$obj") {
            tool(*platformManager.hostPlatform.clangForJni.clangCXX("").toTypedArray())
            flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }

    }
    sourceSet {
        "main" {
            dir("src/main/cpp")
        }
    }
    val objSet = sourceSets["main"]!!.transform(".cpp" to ".$obj")

    target(lib("llvmext"), objSet) {
        tool(*platformManager.hostPlatform.clangForJni.llvmAr("").toTypedArray())
        flags("-qv", ruleOut(), *ruleInAll())
    }
}


val printLlvmDir by tasks.registering {
    doLast {
        println(project.findProperty("llvmDir"))
    }
}
