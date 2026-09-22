plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    iosX64()
    iosArm64()

    applyDefaultHierarchyTemplate()

    /* first cinterop commonizer group */
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
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
