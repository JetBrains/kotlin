import org.jetbrains.kotlin.konan.target.TargetWithSanitizer

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

bitcode {
    hostTarget {
        module("env") {
            srcRoot.set(layout.projectDirectory.dir("src"))
            headersDirs.from(srcRoot.dir("headers"))
            sourceSets {
                main {}
            }
        }
    }
}

kotlinNativeInterop {
    create("env") {
        pkg("org.jetbrains.kotlin.backend.konan.env")
        linker("clang++")
        linkOutputs(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("src/headers/Env.h"))
    }
}


dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir(kotlinNativeInterop["env"].genTask.map { layout.buildDirectory.dir("nativeInteropStubs/env/kotlin") })
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
        builtBy(kotlinNativeInterop["env"].genTask)
    }
}