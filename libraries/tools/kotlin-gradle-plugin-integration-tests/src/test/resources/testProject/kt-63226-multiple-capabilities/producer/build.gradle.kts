plugins {
    `java-library`
    `maven-publish`
}

group = "test"
version = "1.0"

publishing {
    publications {
        create("maven", MavenPublication::class.java) {
            from(components["java"])
        }
    }

    repositories {
        maven("<localRepo>")
    }
}


java {
    withSourcesJar()
    registerFeature("foo") {
        withSourcesJar()
        usingSourceSet(sourceSets.create("foo"))
        capability("test", "foo", "1.0")
    }
}
