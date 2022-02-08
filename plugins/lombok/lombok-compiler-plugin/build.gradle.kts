description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))

    compileOnly(intellijCore())


    testImplementation(intellijCore())
    testImplementation(commonDependency("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation("org.projectlombok:lombok:1.18.16")

    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar()
testsJar()

sourcesJar()
javadocJar()
