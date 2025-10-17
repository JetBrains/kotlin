import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.nativeDistribution.asProperties
import org.jetbrains.kotlin.nativeDistribution.llvmDistributionSource
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution

plugins {
    kotlin("jvm")
    application
    id("native-dependencies")
}

application {
    mainClass.set("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
}

val testCppRuntime by configurations.creating {
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
    implementation(project(":compiler:util"))
    implementation(project(":compiler:ir.serialization.common"))

    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":kotlin-util-klib")))
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

tasks {
    // Copy-pasted from Indexer build.gradle.kts.
    withType<Test>().configureEach {
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

        // Use ARM64 JDK on ARM64 Mac as required by the K/N compiler.
        // See https://youtrack.jetbrains.com/issue/KTI-2421#focus=Comments-27-12231298.0-0.
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}
