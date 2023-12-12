import java.util.concurrent.CountDownLatch
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

kotlin {
    <SingleNativeTarget>("host")
}