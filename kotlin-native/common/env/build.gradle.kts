import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.solib

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

val library = solib("orgjetbrainskotlinbackendkonanenvstubs")

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

val cppApiElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val cppLinkElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(cppApiElements.name, layout.projectDirectory.dir("src/headers"))
    add(cppLinkElements.name, layout.buildDirectory.file("nativelibs/${TargetWithSanitizer.host}/$library")) {
        builtBy(kotlinNativeInterop["env"].genTask)
    }
    add(cppRuntimeElements.name, layout.buildDirectory.file("nativelibs/${TargetWithSanitizer.host}/$library")) {
        builtBy(kotlinNativeInterop["env"].genTask)
    }
}
