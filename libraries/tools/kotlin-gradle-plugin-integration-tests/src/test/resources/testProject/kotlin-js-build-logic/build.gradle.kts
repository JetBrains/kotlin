plugins {
    id("build.logic") apply false
    id("org.jetbrains.kotlin.multiplatform")
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

configureMpp()