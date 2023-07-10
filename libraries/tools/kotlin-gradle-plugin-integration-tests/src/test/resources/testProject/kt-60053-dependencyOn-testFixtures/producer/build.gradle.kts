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
        maven(rootDir.resolve("repo"))
    }
}


java {
    withSourcesJar()
    registerFeature("foo") {
        usingSourceSet(sourceSets.create("foo"))
    }
}
