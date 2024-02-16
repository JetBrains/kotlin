/*
 * Copyright 2010-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

buildscript {
    apply(from = "$rootDir/kotlin-native/gradle/kotlinGradlePlugin.gradle")
}

plugins {
    kotlin("jvm")
    application
    id("native-dependencies")
}

application {
    mainClass.set("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
}

dependencies {
    implementation(project(":kotlin-native:Interop:Indexer"))
    implementation(project(path = ":kotlin-native:endorsedLibraries:kotlinx.cli", configuration = "jvmRuntimeElements"))

    api(project(":kotlin-stdlib"))
    implementation(project(":kotlinx-metadata-klib"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:ir.serialization.common"))

    testImplementation(kotlinTest("junit"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += listOf(
                "-opt-in=kotlin.ExperimentalUnsignedTypes",
                "-Xskip-metadata-version-check"
            )
        }
    }

    // Copy-pasted from Indexer build.gradle.kts.
    withType<Test>().configureEach {
        val projectsWithNativeLibs = listOf(
                project(":kotlin-native:Interop:Indexer"),
                project(":kotlin-native:Interop:Runtime")
        )
        dependsOn(projectsWithNativeLibs.map { "${it.path}:nativelibs" })
        dependsOn(nativeDependencies.llvmDependency)
        systemProperty("java.library.path", projectsWithNativeLibs.joinToString(File.pathSeparator) {
            it.layout.buildDirectory.dir("nativelibs").get().asFile.absolutePath
        })
        val libclangPath = "${nativeDependencies.llvmPath}/" + if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw) {
            "bin/libclang.dll"
        } else {
            "lib/${System.mapLibraryName("clang")}"
        }
        systemProperty("kotlin.native.llvm.libclang", libclangPath)
        systemProperty("kotlin.native.interop.stubgenerator.temp", layout.buildDirectory.dir("stubGeneratorTestTemp").get().asFile)

        // Set the konan.home property because we run the cinterop tool not from a distribution jar
        // so it will not be able to determine this path by itself.
        systemProperty("konan.home", project.project(":kotlin-native").projectDir)
        environment["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
    }
}
