plugins {
    kotlin("multiplatform")
}

kotlin {
    listOf(linuxX64(), linuxArm64(), mingwX64()).forEach {
        it.compilations.getByName("main") {
            cinterops.create("dummy") {
                val nativeLibs = rootDir.resolve("native-libs")
                defFile = nativeLibs.resolve("dummy.def")
                compilerOpts += "-I" + nativeLibs.absolutePath
            }
        }
    }
}
