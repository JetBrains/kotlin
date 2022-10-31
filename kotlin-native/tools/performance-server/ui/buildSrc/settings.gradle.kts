pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
            metadataSources {
                artifact()
            }
        }
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
    val buildGradlePluginVersion = extra["kotlin.build.gradlePlugin.version"]
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    }
}
