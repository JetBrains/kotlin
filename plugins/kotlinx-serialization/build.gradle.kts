import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jsonJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val coreJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:fir:plugin-utils"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":js:js.tests"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":kotlinx-serialization-compiler-plugin.common"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k1"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.k2"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.backend"))
    testImplementation(project(":kotlinx-serialization-compiler-plugin.cli"))

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    coreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0") { isTransitive = false }
    jsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") { isTransitive = false }

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

optInToExperimentalCompilerApi()

publish {
    artifactId = artifactId.replace("kotlinx-", "kotlin-")
}

val archiveName = "kotlin-serialization-compiler-plugin"
val archiveCompatName = "kotlinx-serialization-compiler-plugin"

val runtimeJar = runtimeJar {
    archiveBaseName.set(archiveName)
}

sourcesJar()
javadocJar()
testsJar()
useD8Plugin()

val distCompat by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val compatJar = tasks.register<Copy>("compatJar") {
    from(runtimeJar)
    into(layout.buildDirectory.dir("libsCompat"))
    rename {
        it.replace("kotlin-", "kotlinx-")
    }
}

artifacts {
    add(distCompat.name, layout.buildDirectory.dir("libsCompat").map { it.file("$archiveCompatName-$version.jar") }) {
        builtBy(runtimeJar, compatJar)
    }
}

projectTest(
    parallel = true,
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)
) {
    workingDir = rootDir
    useJUnitPlatform()
    setUpJsIrBoxTests()
    setupCompilerTests()
}

val generateTests by generator("org.jetbrains.kotlinx.serialization.TestGeneratorKt")

abstract class LocalJsCoreArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val localJsCoreRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val localJsJsonRuntimeForTests: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        println(listOf("CRISTIAN",
                       "-Dserialization.core.path=${localJsCoreRuntimeForTests.asPath}",
                       "-Dserialization.json.path=${localJsJsonRuntimeForTests.asPath}",
        ))
        return listOf(
            "-Dserialization.core.path=${localJsCoreRuntimeForTests.asPath}",
            "-Dserialization.json.path=${localJsJsonRuntimeForTests.asPath}",
        )
    }
}

abstract class TestCompilerRuntimeArgumentProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val stdlibRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibMinimalRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val stdlibCommonRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val scriptRuntimeForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinTestJarForTests: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val kotlinReflectJarForTests: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        println(
            listOf(
                "CRISTIAN",
                "-Dkotlin.full.stdlib.path=${stdlibRuntimeForTests.asPath}",
                "-Dkotlin.minimal.stdlib.path=${stdlibMinimalRuntimeForTests.asPath}",
                "-Dkotlin.common.stdlib.path=${stdlibCommonRuntimeForTests.asPath}",
                "-Dkotlin.script.runtime.path=${scriptRuntimeForTests.asPath}",
                "-Dkotlin.test.jar.path=${kotlinTestJarForTests.asPath}",
                "-Dkotlin.reflect.jar.path=${kotlinReflectJarForTests.asPath}",
            )
        )
        return listOf(
            "-Dkotlin.full.stdlib.path=${stdlibRuntimeForTests.asPath}",
            "-Dkotlin.minimal.stdlib.path=${stdlibMinimalRuntimeForTests.asPath}",
            "-Dkotlin.common.stdlib.path=${stdlibCommonRuntimeForTests.asPath}",
            "-Dkotlin.script.runtime.path=${scriptRuntimeForTests.asPath}",
            "-Dkotlin.test.jar.path=${kotlinTestJarForTests.asPath}",
            "-Dkotlin.reflect.jar.path=${kotlinReflectJarForTests.asPath}",
        )
    }
}

val stdlibRuntimeForTests: Configuration by configurations.creating {
    isTransitive = false
    attributes {
        //attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        //attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}
val stdlibMinimalRuntimeForTests: Configuration by configurations.creating {
    isTransitive = false
}
val stdlibCommonRuntimeForTests: Configuration by configurations.creating {
    isTransitive = false
}
val scriptRuntimeForTests: Configuration by configurations.creating {
    isTransitive = false
}
val kotlinTestJarForTests: Configuration by configurations.creating {
    isTransitive = false
}
val kotlinReflectJarForTests: Configuration by configurations.creating {
    isTransitive = false
}
dependencies {
    stdlibRuntimeForTests(project(":kotlin-stdlib"))
    stdlibMinimalRuntimeForTests(project(":kotlin-stdlib-jvm-minimal-for-test"))
    stdlibCommonRuntimeForTests(project(":kotlin-stdlib-common"))
    scriptRuntimeForTests(project(":kotlin-script-runtime"))
    kotlinTestJarForTests(kotlinTest())
    kotlinReflectJarForTests(project(":kotlin-reflect"))
}
fun Test.setupCompilerTests() {
    jvmArgumentProviders.add(
        objects.newInstance(TestCompilerRuntimeArgumentProvider::class.java).apply {
            stdlibRuntimeForTests.from(configurations.named("stdlibRuntimeForTests"))
            stdlibMinimalRuntimeForTests.from(configurations.named("stdlibMinimalRuntimeForTests"))
            stdlibCommonRuntimeForTests.from(configurations.named("stdlibCommonRuntimeForTests"))
            scriptRuntimeForTests.from(configurations.named("scriptRuntimeForTests"))
            kotlinTestJarForTests.from(configurations.named("kotlinTestJarForTests"))
            kotlinReflectJarForTests.from(configurations.named("kotlinReflectJarForTests"))
        }
    )
}

fun Test.setUpJsIrBoxTests() {
    useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

    jvmArgumentProviders.add(
        objects.newInstance(LocalJsCoreArgumentProvider::class.java).apply {
            localJsCoreRuntimeForTests.from(coreJsIrRuntimeForTests)
            localJsJsonRuntimeForTests.from(jsonJsIrRuntimeForTests)
        }
    )
}
