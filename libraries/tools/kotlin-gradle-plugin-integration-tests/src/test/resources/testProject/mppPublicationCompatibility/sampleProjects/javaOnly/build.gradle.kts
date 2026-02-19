plugins {
    `java-library`
    `maven-publish`
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}