plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation("com.fasterxml.woodstox:woodstox-core:6.5.1")
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

projectTest() {
    dependsOn(":dist")
    workingDir = rootDir
    javaLauncher.set(getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
}

testsJar()