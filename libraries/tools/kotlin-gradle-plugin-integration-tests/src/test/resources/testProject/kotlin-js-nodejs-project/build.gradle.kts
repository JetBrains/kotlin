plugins {
    kotlin("js").version("<pluginMarkerVersion>")
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    target {
        useCommonJs()
        produceExecutable()
        nodejs {
        }
    }
}