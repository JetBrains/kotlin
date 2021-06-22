pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
            metadataSources {
                artifact()
            }
        }
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
            metadataSources {
                artifact()
            }
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.30")
    }
}
