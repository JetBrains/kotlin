plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-test:kotlin-test-junit5"))
    testApiJUnit5()
    testImplementation("org.apache.maven:maven-model:3.8.7")
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
}

testsJar()
