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

    api(kotlinStdlib())
    implementation(project(":kotlinx-metadata-klib"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:ir.serialization.common"))

    testImplementation(kotlinTest("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
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
