plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli"))

    compileOnly(intellijCore())

    testImplementation(intellijCore())
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler"))
    testApi(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}
