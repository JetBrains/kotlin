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

val cflags = listOf(
        "-I${layout.projectDirectory.dir("include").asFile}",
        *nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni
)

val ldflags = listOf(
        bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get().outputFile.get().asFile.absolutePath
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
            file(layout.buildDirectory.file("interopTemp/orgjetbrainskotlinbackendkonanenvstubs.c").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(solib("orgjetbrainskotlinbackendkonanenvstubs"), sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags(
                "-shared",
                "-o", ruleOut(), *ruleInAll(),
                *ldflags.toTypedArray())
    }
}

val nativelibs by project.tasks.registering(Sync::class) {
    val lib = solib("orgjetbrainskotlinbackendkonanenvstubs")
    dependsOn(lib)

    from(layout.buildDirectory.dir(lib))
    into(layout.buildDirectory.dir("nativelibs"))
}

kotlinNativeInterop {
    create("env") {
        defFile("env.konan.backend.kotlin.jetbrains.org.def")
        compilerOpts(cflags)
        headers(listOf("Env.h"))

        dependsOn(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get())
    }
}

tasks.named(solib("orgjetbrainskotlinbackendkonanenvstubs")).configure {
    dependsOn(bitcode.hostTarget.module("env").get().sourceSets.main.get().task.get())
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named("orgjetbrainskotlinbackendkonanenvstubs.o").configure {
    dependsOn(kotlinNativeInterop["env"].genTask)
    inputs.file(layout.buildDirectory.file("interopTemp/orgjetbrainskotlinbackendkonanenvstubs.c"))
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
    add(nativeLibs.name, layout.buildDirectory.dir("nativelibs")) {
        builtBy(nativelibs)
    }
}