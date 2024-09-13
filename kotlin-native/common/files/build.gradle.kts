import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.solib

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
    id("native-dependencies")
}

val library = solib("orgjetbrainskotlinbackendkonanfilesstubs")

bitcode {
    hostTarget {
        module("files") {
            sourceSets {
                main {
                    headersDirs.from("include")
                    inputFiles.from("src")
                }
            }
        }
    }
}

val includeFlags = listOf(
        "-I${layout.projectDirectory.dir("include").asFile}",
        *nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni
)

val ldflags = listOf(
        bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get().outputFile.get().asFile.absolutePath
)

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*includeFlags.toTypedArray(),
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            file(layout.buildDirectory.file("nativeInteropStubs/files/c/stubs.c").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(library, sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags(
                "-shared",
                "-o", ruleOut(), *ruleInAll(),
                *ldflags.toTypedArray())
    }
}

kotlinNativeInterop.create("files").genTask.configure {
    defFile.set(layout.projectDirectory.file("files.konan.backend.kotlin.jetbrains.org.def"))
    headersDirs.from(layout.projectDirectory.dir("include"))
}

tasks.named(library).configure {
    dependsOn(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named("stubs.o").configure {
    inputs.file(kotlinNativeInterop["files"].genTask.map { it.cBridge })
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
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(cppApiElements.name, layout.projectDirectory.dir("include"))
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}