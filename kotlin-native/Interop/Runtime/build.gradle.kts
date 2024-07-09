/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("native")
    id("native-dependencies")
}

native {
    val isWindows = PlatformInfo.isWindows()
    val obj = if (isWindows) "obj" else "o"
    val lib = if (isWindows) "lib" else "a"
    val cflags = mutableListOf("-I${nativeDependencies.libffiPath}/include",
                               *hostPlatform.clangForJni.hostCompilerArgsForJni)
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags( *cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "callbacks" {
            dir("src/callbacks/c")
        }
    }
    val objSet = sourceSets["callbacks"]!!.transform(".c" to ".$obj")

    val ldflags = mutableListOf<String>()
    if (HostManager.hostIsMac) {
        ldflags.addAll(listOf("-Xlinker", "-lto_library", "-Xlinker", "KT-69382"))
    }

    target(solib("callbacks"), objSet) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags("-shared",
              "-o",ruleOut(), *ruleInAll(),
              "-L${project(":kotlin-native:libclangext").layout.buildDirectory.get().asFile}",
              "${nativeDependencies.libffiPath}/lib/libffi.$lib",
              "-lclangext", *ldflags.toTypedArray())
    }
    tasks.named(solib("callbacks")).configure {
        dependsOn(":kotlin-native:libclangext:${lib("clangext")}")
        dependsOn(nativeDependencies.libffiDependency)
    }
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":kotlin-stdlib"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

val prepareSharedSourcesForJvm by tasks.registering(Sync::class) {
    from("src/main/kotlin")
    into(project.layout.buildDirectory.dir("src/main/kotlin"))
}
val prepareKotlinIdeaImport by tasks.registering {
    dependsOn(prepareSharedSourcesForJvm)
}

sourceSets.main.configure {
    kotlin.setSrcDirs(emptyList<String>())
    kotlin.srcDir("src/jvm/kotlin")
    kotlin.srcDir(prepareSharedSourcesForJvm)
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        optIn.addAll(
                listOf(
                        "kotlin.ExperimentalUnsignedTypes",
                        "kotlinx.cinterop.BetaInteropApi",
                        "kotlinx.cinterop.ExperimentalForeignApi",
                )
        )
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}


val nativelibs = project.tasks.create<Copy>("nativelibs") {
    val callbacksSolib = solib("callbacks")
    dependsOn(callbacksSolib)

    from(layout.buildDirectory.dir(callbacksSolib))
    into(layout.buildDirectory.dir("nativelibs"))
}
