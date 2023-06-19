plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    /* first cinterop commonizer group */
    listOf(macosX64(), macosArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("libmacos") {
            headers(file("libmacos.h"))
        }
    }

    /* Second cinterop commonizer group */
    listOf(linuxX64(), linuxArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("liblinux") {
            headers(file("liblinux.h"))
        }
    }
}