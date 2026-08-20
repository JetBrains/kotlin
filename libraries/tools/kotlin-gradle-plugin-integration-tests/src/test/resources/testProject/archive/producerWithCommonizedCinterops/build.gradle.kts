plugins {
    kotlin("multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate()

    /* Both interops are declared on both targets, so they end up in a single cinterop commonizer group */
    listOf(linuxX64(), linuxArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("first") {
            headers(file("first.h"))
        }
        target.compilations.getByName("main").cinterops.create("second") {
            headers(file("second.h"))
        }
    }
}
