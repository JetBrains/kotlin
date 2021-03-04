plugins {
    kotlin("multiplatform")
}

kotlin {
    mingwX64("win32") {
        binaries {
            executable {
                entryPoint = "sample.win32.main"
                linkerOpts("-Wl,--subsystem,windows")
            }
        }
    }
}
