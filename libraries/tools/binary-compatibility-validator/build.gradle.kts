plugins {
    kotlin("jvm")
}

val testArtifacts by configurations.creating

dependencies {
    api("org.jetbrains.kotlinx:binary-compatibility-validator:0.15.1")
    api(libs.kotlinx.metadataJvm)

    testApi(kotlinTest("junit"))

    testArtifacts(project(":kotlin-stdlib"))
    testArtifacts(project(":kotlin-stdlib-jdk7"))
    testArtifacts(project(":kotlin-stdlib-jdk8"))
    testArtifacts(project(":kotlin-reflect"))
}

sourceSets {
    "test" {
        java {
            srcDir("src/test/kotlin")
        }
    }
}

val test by tasks.existing(Test::class) {
    dependsOn(testArtifacts)

    systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    systemProperty("testCasesClassesDirs", sourceSets["test"].output.classesDirs.asPath)
    jvmArgs("-ea")
}
