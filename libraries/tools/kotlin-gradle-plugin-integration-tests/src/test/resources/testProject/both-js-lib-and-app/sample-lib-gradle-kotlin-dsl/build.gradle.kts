plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "com.example"
version = "1.0"

kotlin {
    val js = js("nodeJs")

    targets.all {
        mavenPublication(Action<MavenPublication> {
            pom.withXml(Action<XmlProvider> {
                asNode().appendNode("name", "Sample MPP library")
            })
        })
    }
}

publishing {
    repositories {
        maven("<localRepo>") {
            name = "LocalRepo"
        }
    }
}
