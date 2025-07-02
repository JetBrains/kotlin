import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

plugins {
    kotlin("jvm")
}

val testArtifacts by configurations.creating
val nativeStdlibConfiguration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

dependencies {
    api(libs.kotlinx.bcv)
    runtimeOnly("org.ow2.asm:asm-tree:9.7")
    runtimeOnly("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        runtimeOnly(project(":kotlin-compiler-embeddable"))
    } else {
        runtimeOnly(kotlin("compiler-embeddable", bootstrapKotlinVersion))
    }

    testApi(kotlinTest("junit"))

    testArtifacts(project(":kotlin-stdlib"))
    testArtifacts(project(":kotlin-stdlib-jdk7"))
    testArtifacts(project(":kotlin-stdlib-jdk8"))
    testArtifacts(project(":kotlin-reflect"))

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        nativeStdlibConfiguration(project(":kotlin-native:runtime"))
    }
}

sourceSets {
    "test" {
        java {
            srcDir("src/test/kotlin")
        }
    }
}

open class TestArgumentsProvider @Inject constructor(
    objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {
    /**
     * Location of the built Native stdlib.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE) // this file collection is a single directory
    @get:Optional
    val nativeStdlib = objectFactory.fileCollection()

    override fun asArguments() = buildList {
        if (!nativeStdlib.isEmpty) {
            add("-Dnative.stdlib=${nativeStdlib.singleFile.absolutePath}")
        }
    }
}

val test by tasks.existing(Test::class) {
    dependsOn(testArtifacts)
    dependsOn(":kotlin-stdlib:assemble")

    systemProperty("native.enabled", kotlinBuildProperties.isKotlinNativeEnabled)
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    systemProperty("testCasesClassesDirs", sourceSets["test"].output.classesDirs.asPath)
    jvmArgs("-ea")

    // It's currently impossible to use lazy properties with `systemProperty()`. Pass them manually via `CommandLineArgumentProvider`.
    jvmArgumentProviders.add(objects.newInstance<TestArgumentsProvider>().apply {
        nativeStdlib.from(nativeStdlibConfiguration)
    })
}
