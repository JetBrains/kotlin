import org.jetbrains.kotlin.konan.target.TargetWithSanitizer

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

bitcode {
    hostTarget {
        module("env") {
            sourceSets {
                main {
                    headersDirs.from("include")
                    inputFiles.from("src")
                }
            }
        }
    }
}

kotlinNativeInterop {
    create("env") {
        pkg("org.jetbrains.kotlin.backend.konan.env")
        linker("clang++")
        linkOutputs(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("include/Env.h"))
    }
}

configurations.apiElements.configure {
    extendsFrom(kotlinNativeInterop["env"].configuration)
}

configurations.runtimeElements.configure {
    extendsFrom(kotlinNativeInterop["env"].configuration)
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
        builtBy(kotlinNativeInterop["env"].genTask)
    }
}