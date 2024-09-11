import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.solib

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
    id("native-dependencies")
}

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

val cflags = listOf(
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
            flags(*cflags.toTypedArray(),
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            file(layout.buildDirectory.file("interopTemp/orgjetbrainskotlinbackendkonanfilesstubs.c").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(solib("orgjetbrainskotlinbackendkonanfilesstubs"), sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags(
                "-shared",
                "-o", ruleOut(), *ruleInAll(),
                *ldflags.toTypedArray())
    }
}

val nativelibs by project.tasks.registering(Sync::class) {
    val lib = solib("orgjetbrainskotlinbackendkonanfilesstubs")
    dependsOn(lib)

    from(layout.buildDirectory.dir(lib))
    into(layout.buildDirectory.dir("nativelibs"))
}

kotlinNativeInterop {
    create("files") {
        defFile("files.konan.backend.kotlin.jetbrains.org.def")
        compilerOpts(cflags)
        headers(listOf("Files.h"))

        dependsOn(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
    }
}

tasks.named(solib("orgjetbrainskotlinbackendkonanfilesstubs")).configure {
    dependsOn(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named("orgjetbrainskotlinbackendkonanfilesstubs.o").configure {
    dependsOn(kotlinNativeInterop["files"].genTask)
    inputs.file(layout.buildDirectory.file("interopTemp/orgjetbrainskotlinbackendkonanfilesstubs.c"))
}

configurations.apiElements.configure {
    extendsFrom(kotlinNativeInterop["files"].configuration)
}

configurations.runtimeElements.configure {
    extendsFrom(kotlinNativeInterop["files"].configuration)
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
    add(nativeLibs.name, layout.buildDirectory.dir("nativelibs")) {
        builtBy(nativelibs)
    }
}