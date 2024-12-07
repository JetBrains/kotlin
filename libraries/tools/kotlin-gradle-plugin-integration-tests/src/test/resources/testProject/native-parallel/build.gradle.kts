import java.util.concurrent.CountDownLatch
plugins {
    id("org.jetbrains.kotlin.multiplatform").apply(false)
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
