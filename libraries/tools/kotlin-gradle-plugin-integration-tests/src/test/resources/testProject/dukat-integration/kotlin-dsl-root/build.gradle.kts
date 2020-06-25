plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(npm("left-pad", "1.3.0", false))
    implementation(npm("decamelize", "4.0.0"))
}

kotlin {
    js {
        nodejs()
    }
}