import org.jetbrains.kotlin.nativeDistribution.asNativeDistribution
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

description = "Embeddable JAR of Kotlin/Native compiler"
group = "org.jetbrains.kotlin"

val kotlinNativeEmbedded = configurations.dependencyScope("kotlinNativeEmbedded")
val kotlinNativeEmbeddedClasspath = configurations.resolvable("kotlinNativeEmbeddableClasspath") {
    extendsFrom(kotlinNativeEmbedded.get())
}

val kotlinNativeDocumentation = configurations.create("kotlinNativeDocumentation") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Runtime"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:Indexer"))
    kotlinNativeEmbedded(project(":kotlin-native:Interop:StubGenerator"))
    kotlinNativeEmbedded(project(":kotlin-native:backend.native"))
    kotlinNativeEmbedded(project(":kotlin-native:utilities:cli-runner"))
    kotlinNativeEmbedded(project(":kotlin-native:klib"))
    kotlinNativeEmbedded(project(":native:cli-native"))
    kotlinNativeEmbedded(project(":kotlin-native:endorsedLibraries:kotlinx.cli", "jvmRuntimeElements"))

    kotlinNativeDocumentation(project(":kotlin-native:backend.native"))
    kotlinNativeDocumentation(project(":native:cli-native"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlinStdlib())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val compiler = embeddableCompiler("kotlin-native-compiler-embeddable") {
    configurations.add(kotlinNativeEmbeddedClasspath)
    exclude("com/sun/jna/**")
    mergeServiceFiles()
    // Shadow uses by default 'DuplicatesStrategy.EXCLUDE' which prevents duplicate service files from being processed,
    // see https://gradleup.com/shadow/configuration/merging/#handling-duplicates-strategy
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

val runtimeJar = runtimeJar(compiler)

val sourcesJar = sourcesJar {
    addEmbeddedSources("kotlinNativeDocumentation")
}

val javadocJar = javadocJar {
    addEmbeddedJavadoc("kotlinNativeDocumentation")
}

publish {
    setArtifacts(listOf(runtimeJar, sourcesJar, javadocJar))
}

sourceSets {
    "main" {}
    "test" {
        kotlin {
            srcDir("tests/kotlin")
        }
    }
}

open class ProjectTestArgumentProvider @Inject constructor(
        objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {
    @get:Classpath
    val compilerClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val nativeDistributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    private val nativeDistribution = nativeDistributionRoot.asNativeDistribution()

    override fun asArguments(): Iterable<String> = listOf(
            "-DcompilerClasspath=${compilerClasspath.files.joinToString(separator = File.pathSeparator) { it.absolutePath }}",
            "-Dkotlin.native.home=${nativeDistribution.get().root.asFile.absolutePath}",
    )
}

projectTests {
    testData(isolated, "testData")

    testTask {
        /**
         * It's expected that test should be executed on CI, but currently this project under `kotlin.native.enabled`
         */
        jvmArgumentProviders.add(objects.newInstance<ProjectTestArgumentProvider>().apply {
            compilerClasspath.from(runtimeJar)

            // The tests run the compiler and try to produce an executable on host.
            // So, distribution with stdlib and runtime for host is required.
            nativeDistributionRoot.set(project.nativeDistribution.map { it.root })
            dependsOn(":kotlin-native:distRuntime")
        })
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
    }
}
