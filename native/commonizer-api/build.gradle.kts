import org.jetbrains.kotlin.nativeDistribution.useProvidedNativeBootstrapDistribution

plugins {
    kotlin("jvm")
    id("gradle-plugin-published-compiler-dependency-configuration")
    id("project-tests-convention")
    id("native-bootstrap-distribution-provisioner")
    id("test-inputs-check")
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
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useProvidedNativeBootstrapDistribution { distribution ->
            addClasspathProperty("konan.home") {
                from(distribution.map { it.root })
            }
        }

        javaLauncher = getToolchainLauncherFor(JdkMajorVersion.JDK_21_0)
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        testData(project.isolated, "testData")
        testInputsCheck {
            extraPermissions.add("""permission java.util.PropertyPermission "*", "read,write";""")
        }
    }
}

runtimeJar()
sourcesJar()
javadocJar()
