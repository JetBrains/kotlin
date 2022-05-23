description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common"))
    embedded(project(":kotlin-lombok-compiler-plugin.k1"))
    embedded(project(":kotlin-lombok-compiler-plugin.cli"))

    testImplementation(intellijCore())
    testImplementation(project(":kotlin-lombok-compiler-plugin.common"))
    testImplementation(project(":kotlin-lombok-compiler-plugin.k1"))
    testImplementation(project(":kotlin-lombok-compiler-plugin.cli"))

    testImplementation(commonDependency("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation("org.projectlombok:lombok:1.18.16")

    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
