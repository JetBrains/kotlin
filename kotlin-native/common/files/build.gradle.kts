import org.jetbrains.kotlin.konan.target.TargetWithSanitizer

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

bitcode {
    hostTarget {
        module("files") {
            srcRoot.set(layout.projectDirectory.dir("src"))
            headersDirs.from(srcRoot.dir("headers"))
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
        linkOutputs(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("src/headers/Files.h"))
    }
}


dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir(kotlinNativeInterop["files"].genTask.map { layout.buildDirectory.dir("nativeInteropStubs/files/kotlin") })
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
        builtBy(kotlinNativeInterop["files"].genTask)
    }
}