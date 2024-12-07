plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64("ios") {
        binaries {
            framework("mainDynamic") {
                isStatic = false
            }

            if (properties.containsKey("multipleFrameworks")) {
                framework("mainStatic") {
                    isStatic = true
                }
            }
        }
    }
}