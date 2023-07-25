plugins {
    kotlin("js")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.thirdparty"
version = "1.0"

kotlin {
    js()

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        js().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}