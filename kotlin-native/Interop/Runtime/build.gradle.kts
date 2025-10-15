/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.libname

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("native")
    id("native-dependencies")
}

val library = solib("callbacks")

val cppLink by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

dependencies {
    cppLink(project(":kotlin-native:libclangext"))
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

    target(library, objSet) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        val dynamicLibs = buildList {
            cppLink.incoming.artifactView {
                attributes {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
                }
            }.files.flatMapTo(this) { listOf("-L${it.parentFile.toRelativeString(workingDir.asFile.get())}", "-l${libname(it)}") }
        }
        flags("-shared",
              "-o",ruleOut(), *ruleInAll(),
              "${nativeDependencies.libffiPath}/lib/libffi.$lib",
                *dynamicLibs.toTypedArray())

        if (HostManager.hostIsMac) {
            // Set install_name to a non-absolute path.
            flags("-Wl,-install_name,@rpath/$library")
        }
        if (HostManager.hostIsMingw) {
            // Use binary hash as the timestamp in COFF headers.
            flags("-Wl,/Brepro")
        }
    }
    tasks.named(library).configure {
        inputs.files(cppLink).withPathSensitivity(PathSensitivity.NONE)
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
        freeCompilerArgs.add("-Xreturn-value-checker=full")
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
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}
