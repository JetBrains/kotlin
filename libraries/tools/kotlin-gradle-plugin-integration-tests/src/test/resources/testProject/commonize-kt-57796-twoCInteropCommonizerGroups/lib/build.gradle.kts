plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    // fixme: KT-89587 Clean up tests after iosX64 target deprecation
    iosX64()
    iosArm64()

    applyDefaultHierarchyTemplate()

    /* first cinterop commonizer group */
    // fixme: KT-89587 Clean up tests after iosX64 target deprecation
    listOf(iosX64(), iosArm64()).forEach { target ->
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
