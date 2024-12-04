plugins {
    id("kotlin")
}

repositories {
    mavenCentral()
}

val testArtifacts by configurations.creating
val signature by configurations.creating

sourceSets {
    "main" { none() }
    "test" { kotlin.srcDir("src/test") }
}

dependencies {
    implementation("org.codehaus.mojo:animal-sniffer:1.21")
    implementation(kotlinStdlib())

    testImplementation(kotlinTest("junit"))

    testArtifacts(project(":kotlin-reflect"))

    signature("org.codehaus.mojo.signature:java16:1.1@signature")
}

val signaturesDirectory = layout.buildDirectory.get().asFile.resolve("signatures")

val collectSignatures by tasks.registering(Sync::class) {
    from(signature)
    into(signaturesDirectory)
}

tasks.getByName<Test>("test") {
    dependsOn(collectSignatures)
    dependsOn(testArtifacts)

    systemProperty("kotlinVersion", project.version)
    systemProperty("signaturesDirectory", signaturesDirectory)
}
