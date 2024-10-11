import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.obj
import org.jetbrains.kotlin.tools.solib

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
}

val stubsName = "orgjetbrainskotlinbackendkonanenvstubs"
val library = solib(stubsName)
val objFile = obj(stubsName)
val cFile = "$stubsName.c"

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

val cflags = listOf("-I${layout.projectDirectory.dir("src/headers").asFile}", *nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni)

val ldflags = listOf(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get().outputFile.get().asFile.absolutePath)

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            file(layout.buildDirectory.file("interopTemp/$cFile").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(library, sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags("-shared", "-o", ruleOut(), *ruleInAll(), *ldflags.toTypedArray())
    }
}

kotlinNativeInterop {
    create("env") {
        pkg("org.jetbrains.kotlin.backend.konan.env")
        headers(layout.projectDirectory.files("src/headers/Env.h"))
        skipNatives()
    }
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named(library).configure {
    inputs.file(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.map { it.outputFile })
}
tasks.named(objFile).configure {
    inputs.file(kotlinNativeInterop["env"].genTask.map { layout.buildDirectory.file("interopTemp/$cFile") })
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
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}