@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("jvm")
    `maven-publish`
}


group = "org.jetbrains.sample"
version = "1.0.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.jetbrains.sample"
            artifactId = "producerA"
            version = "1.0.0-SNAPSHOT"
            from(components["java"])
        }
    }
}
