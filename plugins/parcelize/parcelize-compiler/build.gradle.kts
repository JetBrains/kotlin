import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType

description = "Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("android-sdk-provisioner")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val robolectricClasspath by configurations.creating
val robolectricDependency by configurations.creating

val parcelizeRuntimeForTests by configurations.creating
val layoutLib by configurations.creating
val layoutLibApi by configurations.creating

dependencies {
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

    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:ir.backend.common"))
    testFixturesApi(project(":compiler:backend.jvm"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":plugins:parcelize:parcelize-runtime"))
    testFixturesApi(kotlinTest())

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    // FIR dependencies
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.js"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.native"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    testRuntimeOnly(toolsJar())
    testFixturesApi(libs.junit4)

    robolectricDependency("org.robolectric:android-all:5.0.2_r3-robolectric-r0")

    robolectricClasspath(commonDependency("org.robolectric", "robolectric"))
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
