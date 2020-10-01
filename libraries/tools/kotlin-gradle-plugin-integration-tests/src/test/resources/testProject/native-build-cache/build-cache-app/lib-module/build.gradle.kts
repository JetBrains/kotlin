plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    <SingleNativeTarget>("host")
}
