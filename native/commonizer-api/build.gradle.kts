import org.jetbrains.kotlin.nativeDistribution.useProvidedNativeBootstrapDistribution

plugins {
    kotlin("jvm")
    id("gradle-plugin-published-compiler-dependency-configuration")
    id("project-tests-convention")
    id("native-bootstrap-distribution-provisioner")
    id("test-inputs-check-v2")
}

kotlin {
    explicitApi()
}

description = "Kotlin KLIB Library Commonizer API"
publish()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":native:kotlin-native-utils"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testRuntimeOnly(project(":native:kotlin-klib-commonizer"))
    testImplementation(project(":kotlin-gradle-statistics"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_21_0) {
        useProvidedNativeBootstrapDistribution { distribution ->
            addClasspathProperty("konan.home") {
                from(distribution.map { it.root })
            }
        }
        testData(project.isolated, "testData")
    }
}

runtimeJar()
sourcesJar()
javadocJar()
