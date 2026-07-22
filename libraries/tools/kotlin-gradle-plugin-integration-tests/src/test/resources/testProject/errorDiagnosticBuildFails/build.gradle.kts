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
        val myCustomSourceSet = create("myCustomSourceSet")
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}

tasks.create("myTask") {
    println("Custom Task Executed")
}
