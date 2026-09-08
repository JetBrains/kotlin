plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

group = "MultiProject"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("host")
}
