plugins {
    id("kotlin")
}

repositories {
    mavenCentral()
}

val testArtifacts by configurations.creating
val signature by configurations.creating

dependencies {
    api("org.codehaus.mojo:animal-sniffer:1.21")
    api(project(":kotlin-stdlib-jdk8"))
    testApi(project(":kotlin-test:kotlin-test-junit"))

    testArtifacts(project(":kotlin-stdlib"))
    testArtifacts(project(":kotlin-reflect"))

    signature("org.codehaus.mojo.signature:java16:1.1@signature")
}

val signaturesDirectory = buildDir.resolve("signatures")

val collectSignatures by tasks.registering(Sync::class) {
    dependsOn(signature)
    from(signature)
    into(signaturesDirectory)
}

tasks.getByName<Test>("test") {
    dependsOn(collectSignatures)
    dependsOn(testArtifacts)

    systemProperty("kotlinVersion", project.version)
    systemProperty("signaturesDirectory", signaturesDirectory)
}
