description = "kotlin-gradle-statistics"

plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
}

dependencies {
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit:junit"))
}

projectTest {
    workingDir = rootDir
}
