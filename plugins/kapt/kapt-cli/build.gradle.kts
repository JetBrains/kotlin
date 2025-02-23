plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))

    compileOnly(intellijCore())
    compileOnly(kotlin("stdlib"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))


    testImplementation(intellijCore())
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar()

projectTest {
    useJUnitPlatform()
    workingDir = rootDir
    dependsOn(":dist")
    val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
    doFirst {
        environment("JAVA_HOME", jdkHome.get())
    }
}
