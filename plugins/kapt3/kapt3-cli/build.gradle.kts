plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli"))

    compileOnly(intellijCore())

    testImplementation(intellijCore())
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApiJUnit5()
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
}
