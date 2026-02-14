import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import java.util.zip.ZipFile

description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("android-sdk-provisioner")
    id("java-test-fixtures")
    id("project-tests-convention")
}

repositories {
    google()
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

val robolectricClasspath by configurations.creating {
    attributes {
        attribute(Attribute.of("artifactType", String::class.java), "jar")
    }
}
val robolectricDependency by configurations.creating

val parcelizeRuntimeForTests by configurations.creating
val layoutLib by configurations.creating
val layoutLibApi by configurations.creating

dependencies {
    registerTransform(AarToJarTransform::class.java) {
        from.attribute(Attribute.of("artifactType", String::class.java), "aar")
        to.attribute(Attribute.of("artifactType", String::class.java), "jar")
    }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.common")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.k1")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.k2")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.backend")) { isTransitive = false }
    embedded(project(":plugins:parcelize:parcelize-compiler:parcelize.cli")) { isTransitive = false }

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(intellijCore())

    testFixturesApi(project(":plugins:parcelize:parcelize-compiler:parcelize.cli"))

    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesApi(project(":plugins:parcelize:parcelize-runtime"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    testRuntimeOnly(toolsJar())
    testFixturesApi(libs.junit4)

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
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

val robolectricDependencyDir = layout.buildDirectory.dir("robolectricDependencies")
val prepareRobolectricDependencies by tasks.registering(Copy::class) {
    from(robolectricDependency)
    into(robolectricDependencyDir)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(parcelizeRuntimeForTests)
        dependsOn(robolectricClasspath)
        dependsOn(robolectricDependency)

        dependsOn(prepareRobolectricDependencies)
        dependsOn(":dist")
        workingDir = rootDir
        androidSdkProvisioner {
            provideToThisTaskAsSystemProperty(ProvisioningType.PLATFORM_JAR)
        }

        val parcelizeRuntimeForTestsConf: FileCollection = parcelizeRuntimeForTests
        val robolectricClasspathConf: FileCollection = robolectricClasspath
        val robolectricDependencyDir: Provider<Directory> = robolectricDependencyDir
        val layoutLibConf: FileCollection = layoutLib
        val layoutLibApiConf: FileCollection = layoutLibApi
        doFirst {
            systemProperty("parcelizeRuntime.classpath", parcelizeRuntimeForTestsConf.asPath)
            systemProperty("robolectric.classpath", robolectricClasspathConf.asPath)

            systemProperty("robolectric.offline", "true")
            systemProperty("robolectric.dependency.dir", robolectricDependencyDir.get().asFile)

            systemProperty("layoutLib.path", layoutLibConf.singleFile.canonicalPath)
            systemProperty("layoutLibApi.path", layoutLibApiConf.singleFile.canonicalPath)
        }
    }

    testGenerator("org.jetbrains.kotlin.parcelize.test.TestGeneratorKt")

    withJvmStdlibAndReflect()
}
