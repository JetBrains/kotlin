description = "Simple Annotation Processor for testing kapt"

plugins {
    kotlin("jvm")
    id("java-instrumentation")
    `maven-publish` // only used for installing to mavenLocal()
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "test" {}
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}

tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}
