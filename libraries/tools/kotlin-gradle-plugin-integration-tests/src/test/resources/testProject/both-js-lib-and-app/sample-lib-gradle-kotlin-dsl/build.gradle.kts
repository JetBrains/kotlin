plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "com.example"
version = "1.0"

kotlin {
    js("nodeJs")

    targets {
        all {
            mavenPublication {
                pom.withXml {
                    asNode().appendNode("name", "Sample MPP library")
                }
            }
        }
    }
}

kotlin.sourceSets.forEach { println(it.kotlin.srcDirs) }

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri("<localRepo>")
        }
    }
}
