plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.woodstox.core)
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)

    testImplementation(libs.jgit)
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
    }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
    javaLauncher.set(getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
    jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
}

testsJar()