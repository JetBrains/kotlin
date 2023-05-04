/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.*

plugins {
    kotlin
    id("native")
}

native {
    val isWindows = PlatformInfo.isWindows()
    val obj = if (isWindows) "obj" else "o"
    val lib = if (isWindows) "lib" else "a"
    val host = rootProject.project(":kotlin-native").extra["hostName"]
    val hostLibffiDir = rootProject.project(":kotlin-native").extra["${host}LibffiDir"]
    val cflags = mutableListOf("-I$hostLibffiDir/include",
                               *platformManager.hostPlatform.clangForJni.hostCompilerArgsForJni)
    suffixes {
        (".c" to ".$obj") {
            tool(*platformManager.hostPlatform.clangForJni.clangC("").toTypedArray())
            flags( *cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "callbacks" {
            dir("src/callbacks/c")
        }
    }
    val objSet = sourceSets["callbacks"]!!.transform(".c" to ".$obj")

    target(solib("callbacks"), objSet) {
        tool(*platformManager.hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags("-shared",
              "-o",ruleOut(), *ruleInAll(),
              "-L${project(":kotlin-native:libclangext").buildDir}",
              "$hostLibffiDir/lib/libffi.$lib",
              "-lclangext")
    }
    tasks.named(solib("callbacks")).configure {
        dependsOn(":kotlin-native:libclangext:${lib("clangext")}")
    }
}

dependencies {
    implementation(project(":kotlin-native:utilities:basic-utils"))
    implementation(project(":kotlin-stdlib"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets.main.get().java.srcDir("src/jvm/kotlin")


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=kotlinx.cinterop.BetaInteropApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-Xskip-prerelease-check"
        )
    }
}


val nativelibs = project.tasks.create<Copy>("nativelibs") {
    val callbacksSolib = solib("callbacks")
    dependsOn(callbacksSolib)

    from("$buildDir/$callbacksSolib")
    into("$buildDir/nativelibs/")
}
