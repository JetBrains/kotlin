import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.nativeDistribution.asProperties
import org.jetbrains.kotlin.nativeDistribution.llvmDistributionSource
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
    id("native-dependencies")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

application {
    mainClass.set("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
}

val testCppRuntime = configurations.create("testCppRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

dependencies {
    implementation(project(":kotlin-native:Interop:Indexer"))
    implementation(project(path = ":kotlin-native:endorsedLibraries:kotlinx.cli", configuration = "jvmRuntimeElements"))

    api(kotlinStdlib())
    implementation(project(":kotlinx-metadata-klib"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":native:unsafe-mem"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":kotlin-util-klib-metadata"))

    testImplementation(kotlinTest("junit5"))
    testImplementation(testFixtures(project(":native:kotlin-native-utils")))
    testRuntimeOnly(libs.junit.jupiter.engine)

    testCppRuntime(project(":kotlin-native:libclangInterop"))
    testCppRuntime(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

open class TestArgumentProvider @Inject constructor(
        objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val nativeLibraries: ConfigurableFileCollection = objectFactory.fileCollection()

    override fun asArguments(): Iterable<String> = listOf(
            "-Djava.library.path=${nativeLibraries.files.joinToString(File.pathSeparator) { it.parentFile.absolutePath }}"
    )
}

projectTests {
    testTask {
        // konan.home points to the kotlin-native project directory for konan.properties; declare it
        // as an input so the cache is properly invalidated when it changes.
        inputs.dir(project(":kotlin-native").isolated.projectDirectory.dir("konan"))
            .withPathSensitivity(PathSensitivity.RELATIVE)
        // Copy-pasted from Indexer build.gradle.kts.
        dependsOn(nativeDependencies.llvmDependency)
        jvmArgumentProviders.add(objects.newInstance<TestArgumentProvider>().apply {
            nativeLibraries.from(testCppRuntime)
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
        systemProperty("kotlin.native.propertyOverrides", llvmDistributionSource.asProperties.entries.joinToString(separator = ";") {
            "${it.key}=${it.value}"
        })
        environment["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
    }
}
