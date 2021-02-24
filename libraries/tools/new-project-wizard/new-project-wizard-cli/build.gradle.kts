plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))

    implementation(project(":libraries:tools:new-project-wizard"))
    implementation("org.yaml:snakeyaml:1.24")

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(project(":kotlin-reflect"))
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
