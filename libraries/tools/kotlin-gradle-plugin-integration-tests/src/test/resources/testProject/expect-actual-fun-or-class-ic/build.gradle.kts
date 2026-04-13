plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js { browser { } }
    <SingleNativeTarget>("native")
}
