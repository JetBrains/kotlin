plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("kotlin")
    id("test-inputs-check")
    id("project-tests-convention")
}

val signature = configurations.create("signature")

sourceSets {
    "main" { none() }
    "test" { kotlin.srcDir("src/test") }
}

dependencies {
    implementation("org.codehaus.mojo:animal-sniffer:1.21")
    implementation(kotlinStdlib())

    testImplementation(kotlinTest("junit5"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))

    signature("org.codehaus.mojo.signature:java16:1.1@signature")
}

val signaturesDirectory = layout.buildDirectory.get().asFile.resolve("signatures")

val collectSignatures = tasks.register("collectSignatures", Sync::class) {
    from(signature)
    into(signaturesDirectory)
}

projectTests {
    testTask {
        systemProperty("kotlinVersion", project.version)
        addDirectoryProperty("signaturesDirectory") {
            fileProvider(collectSignatures.map { it.destinationDir })
        }
        withJvmStdlibAndReflect()
        withReflectShadowJar()
    }
}
