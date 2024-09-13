import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution

plugins {
    kotlin("jvm")
    application
    id("native-dependencies")
}

apply<CppConsumerPlugin>()

val cppRuntimeOnly by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
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

    cppRuntimeOnly(project(":kotlin-native:libclangInterop"))
    cppRuntimeOnly(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks {
    // Copy-pasted from Indexer build.gradle.kts.
    withType<Test>().configureEach {
        inputs.files(cppRuntimeOnly)
        dependsOn(nativeDependencies.llvmDependency)
        systemProperty("java.library.path", cppRuntimeOnly.elements.map { elements ->
            elements.joinToString(File.pathSeparator) { it.asFile.parentFile.absolutePath }
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
        systemProperty("konan.home", nativeProtoDistribution.root.asFile) // at most target description is required in the distribution.
        environment["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
    }
}
