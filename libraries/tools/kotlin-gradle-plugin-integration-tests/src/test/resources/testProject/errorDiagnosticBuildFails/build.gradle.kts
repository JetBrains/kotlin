plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64()
    mingwX64()

    sourceSets {
        val myCustomSourceSet by creating
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}

tasks.create("myTask") {
    println("Custom Task Executed")
}
