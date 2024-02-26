/**
 * Used in for modules in 'native/objcexport-heade-generator/testDependencies.
 * Such libraries can build klibs that can then later be used for running objc export tests against
 */
plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
}
