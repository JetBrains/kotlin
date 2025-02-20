import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.nativeDistribution.NativeDistributionProperty
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import org.jetbrains.kotlin.nativeDistribution.nativeDistributionProperty

plugins {
    kotlin("jvm")
}

description = "Embeddable JAR of Kotlin/Native compiler"
group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

val kotlinNativeEmbedded by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val kotlinNativeSources by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
    }
}

val kotlinNativeJavadoc by configurations.creating {
    isVisible = false
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
    }
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
    kotlinNativeEmbedded(project(":kotlin-compiler")) { isTransitive = false }

    kotlinNativeSources(project(":kotlin-native:backend.native"))
    kotlinNativeSources(project(":native:cli-native"))
    kotlinNativeJavadoc(project(":kotlin-native:backend.native"))
    kotlinNativeJavadoc(project(":native:cli-native"))

    testImplementation(libs.junit4)
    testImplementation(kotlinTest("junit"))
}

val compiler = embeddableCompiler("kotlin-native-compiler-embeddable") {
    from(kotlinNativeEmbedded)
    mergeServiceFiles()
}

val runtimeJar = runtimeJar(compiler) {
    exclude("com/sun/jna/**")
    mergeServiceFiles()
}

val archiveZipper = serviceOf<ArchiveOperations>()::zipTree

val sourcesJar = sourcesJar {
    dependsOn(kotlinNativeSources)
    from { kotlinNativeSources.map { archiveZipper(it) } }
}

val javadocJar = javadocJar {
    dependsOn(kotlinNativeJavadoc)
    from { kotlinNativeJavadoc.map { archiveZipper(it) } }
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
    val nativeDistribution: NativeDistributionProperty = objectFactory.nativeDistributionProperty()

    override fun asArguments(): Iterable<String> = listOf(
            "-DcompilerClasspath=${compilerClasspath.files.joinToString(separator = File.pathSeparator) { it.absolutePath }}",
            "-Dkotlin.native.home=${nativeDistribution.get().root.asFile.absolutePath}",
    )
}

projectTest {
    /**
     * It's expected that test should be executed on CI, but currently this project under `kotlin.native.enabled`
     */
    jvmArgumentProviders.add(objects.newInstance<ProjectTestArgumentProvider>().apply {
        compilerClasspath.from(runtimeJar)

        // The tests run the compiler and try to produce an executable on host.
        // So, distribution with stdlib and runtime for host is required.
        nativeDistribution.set(project.nativeDistribution)
        dependsOn(":kotlin-native:distRuntime")
    })
}