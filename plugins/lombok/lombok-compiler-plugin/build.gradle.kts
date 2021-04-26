description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }


    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testImplementation(commonDep("junit:junit"))
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

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
