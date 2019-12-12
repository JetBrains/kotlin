plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))

    implementation(project(":libraries:tools:new-project-wizard"))
    compileOnly(intellijDep()) { includeJars("snakeyaml-1.24") }

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()
