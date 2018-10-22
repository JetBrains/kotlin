plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:cli"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

testsJar {}

dist()

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}