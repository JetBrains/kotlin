plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64()
    jvm()

    sourceSets {
        val myCustomSourceSet by creating
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}

tasks.create("myTask") {
    println("Custom Task Executed")
}
