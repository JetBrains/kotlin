plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    /* first cinterop commonizer group */
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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