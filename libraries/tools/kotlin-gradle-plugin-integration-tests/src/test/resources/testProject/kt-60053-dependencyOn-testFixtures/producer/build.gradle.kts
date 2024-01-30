plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

group = "org.jetbrains.sample"
version = "1.0.0"

publishing {
    publications {
        create("myLibrary", MavenPublication::class.java) {
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
        usingSourceSet(sourceSets.create("foo"))
    }
}
