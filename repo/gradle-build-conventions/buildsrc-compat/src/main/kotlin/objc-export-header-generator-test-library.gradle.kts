/**
 * Used in for modules in 'native/objcexport-heade-generator/testDependencies.
 * Such libraries can build klibs that can then later be used for running objc export tests against
 */
plugins {
    kotlin("multiplatform")
}

/*
Depends on https://youtrack.jetbrains.com/issue/KT-65985
 */
providers.systemProperty("kotlin.internal.native.test.nativeHome").orNull?.let { nativeHome ->
    extensions.extraProperties.set("kotlin.native.home", nativeHome)
}

kotlin {
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()
}
