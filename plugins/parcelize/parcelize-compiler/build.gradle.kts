import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import java.util.zip.ZipFile

description = "Parcelize compiler plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("android-sdk-provisioner")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

/**
 * Used to unpack the `classes.jar` from `.aar` artifacts.
 *
 * See `androidx.test:monitor` package below.
 */
@CacheableTransform
abstract class AarToJarTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputAar: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val aarFile = inputAar.get().asFile
        ZipFile(aarFile).use { zip ->
            val classesJarEntry = zip.getEntry("classes.jar")
            if (classesJarEntry != null) {
                val outputJar = outputs.file("${aarFile.nameWithoutExtension}-classes.jar")
                zip.getInputStream(classesJarEntry).use { inputStream ->
                    outputJar.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
    }
}

val robolectricClasspath = configurations.create("robolectricClasspath") {
    attributes {
        attribute(Attribute.of("artifactType", String::class.java), "jar")
    }
    resolutionStrategy.eachDependency {
        checkAndOverrideBouncyCastleVersion(project)
    }
}
val robolectricDependency = configurations.create("robolectricDependency")

val parcelizeRuntimeForTests = configurations.create("parcelizeRuntimeForTests")
val layoutLib = configurations.create("layoutLib")
val layoutLibApi = configurations.create("layoutLibApi")

dependencies {
    registerTransform(AarToJarTransform::class.java) {
        from.attribute(Attribute.of("artifactType", String::class.java), "aar")
        to.attribute(Attribute.of("artifactType", String::class.java), "jar")
    }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.common")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.k2")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.backend")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.cli")) { isTransitive = false }

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(project(":plugins:parcelize:parcelize-compiler:parcelize.cli"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    testRuntimeOnly(toolsJar())
    testImplementation(project(":compiler:cli-base"))
    testFixturesImplementation(libs.junit4) // needed for runtime of box tests, see `ParcelizeMainClassProvider`

    // Must be kept in sync with ANDROID_API_VERSION in ParcelizeRuntimeClasspathProvider.
    // The dependency version defined here determines the Android API version.
    robolectricDependency("org.robolectric:android-all-instrumented:16-robolectric-13921718-i7")

    robolectricClasspath(commonDependency("org.robolectric", "robolectric"))

    // This dependency is an `.aar` file.
    robolectricClasspath("androidx.test:monitor:1.8.0")
    robolectricClasspath(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }

    parcelizeRuntimeForTests(project(":plugins:parcelize:parcelize-runtime")) { isTransitive = false }
    parcelizeRuntimeForTests(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm")) { isTransitive = false }

    layoutLib("org.jetbrains.intellij.deps.android.tools:layoutlib:26.5.0") { isTransitive = false }
    layoutLibApi("com.android.tools.layoutlib:layoutlib-api:26.5.0") { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

val projectDir = layout.projectDirectory
val robolectricDependencyDir = layout.buildDirectory.dir("robolectricDependencies")
val prepareRobolectricDependencies = tasks.register("prepareRobolectricDependencies", Copy::class) {
    from(robolectricDependency)
    into(robolectricDependencyDir)
}

projectTests {
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_21_0)) {
        inputs.files(prepareRobolectricDependencies.map { it.outputs })
            .withNormalizer(ClasspathNormalizer::class)
            .withPropertyName("prepareRobolectricDependenciesOutput")

        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.PLATFORM_JAR)
        }

        addClasspathProperty(parcelizeRuntimeForTests, "parcelizeRuntime.classpath")
        addClasspathProperty(robolectricClasspath, "robolectric.classpath")
        addClasspathProperty(layoutLib, "layoutLib.path")
        addClasspathProperty(layoutLibApi, "layoutLibApi.path")
        addClasspathProperty("parcelizePlugin.jar") {
            from(tasks.jar)
        }


        val robolectricDependencyDir: Provider<Directory> = robolectricDependencyDir
        val projectDir = projectDir
        doFirst {
            systemProperty("robolectric.offline", "true")
            systemProperty("robolectric.dependency.dir", robolectricDependencyDir.get().asFile.relativeTo(projectDir.asFile))
        }
    }

    testGenerator("org.jetbrains.kotlin.parcelize.test.TestGeneratorKt", generateTestsInBuildDirectory = true)

    testData(isolated, "testData")

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkAnnotationsJar()
    withScriptRuntime()
}
