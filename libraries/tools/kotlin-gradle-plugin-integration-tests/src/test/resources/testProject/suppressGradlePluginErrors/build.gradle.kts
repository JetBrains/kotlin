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
        val myCustomSourceSet = create("myCustomSourceSet")
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}

tasks.create("myTask") {
    println("Custom Task Executed")
}
