plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api("org.apache.velocity:velocity:1.7") // we have to use the old version as it is the same as bundled into IntelliJ
    compileOnly(project(":kotlin-reflect-api"))

    //needed only for message bundles
    implementation(intellijDep()) { includeJars("util") }

    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDep("junit:junit"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${property("versions.kotlinx-collections-immutable")}")
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
