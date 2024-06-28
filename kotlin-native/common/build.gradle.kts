import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.PlatformInfo

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("kotlin.native.build-tools-conventions")
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

bitcode {
    // These are only used in kotlin-native/backend.native/build.gradle where only the host target is needed.
    hostTarget {
        module("files") {
            headersDirs.from(layout.projectDirectory.dir("src/files/headers"))
            sourceSets {
                main {}
            }
        }
        module("env") {
            headersDirs.from(layout.projectDirectory.dir("src/env/headers"))
            sourceSets {
                main {}
            }
        }
    }
}

kotlinNativeInterop {
    create("files") {
        pkg("org.jetbrains.kotlin.backend.konan.files")
        linker("clang++")
        if (PlatformInfo.isMac()) {
            linkerOpts("-Xlinker", "-lto_library", "-Xlinker", "KT-69382")
        }
        linkOutputs(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("src/files/headers/Files.h"))
    }

    create("env") {
        pkg("org.jetbrains.kotlin.backend.konan.env")
        linker("clang++")
        if (PlatformInfo.isMac()) {
            linkerOpts("-Xlinker", "-lto_library", "-Xlinker", "KT-69382")
        }
        linkOutputs(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("src/env/headers/Env.h"))
    }
}

val nativeLibs by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(nativeLibs.name, layout.buildDirectory.dir("nativelibs/${TargetWithSanitizer.host}")) {
        builtBy(kotlinNativeInterop["files"].genTask, kotlinNativeInterop["env"].genTask)
    }
}