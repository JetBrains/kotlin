plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64()
    mingwX64()
    macosX64()
}

dependencies {
    //compileOnly: commonMainCompileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}
