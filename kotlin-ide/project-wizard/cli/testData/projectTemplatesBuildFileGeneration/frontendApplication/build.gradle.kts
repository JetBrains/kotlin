plugins {
    kotlin("js") version "KOTLIN_VERSION"
}
group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers")
    }
}
dependencies {
    testImplementation(kotlin("test-js"))
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    implementation("org.jetbrains:kotlin-react:16.13.0-pre.93-kotlin-KOTLIN_VERSION")
    implementation("org.jetbrains:kotlin-react-dom:16.13.0-pre.93-kotlin-KOTLIN_VERSION")
    implementation(npm("react","16.13.0"))
    implementation(npm("react-dom","16.13.0"))
    implementation(npm("react-is","16.13.0"))
    implementation("org.jetbrains:kotlin-styled:1.0.0-pre.93-kotlin-KOTLIN_VERSION")
    implementation(npm("styled-components","5.0.0"))
    implementation(npm("inline-style-prefixer","5.1.0"))
}
kotlin {
    js {
        browser {}
        binaries.executable()
    }
}
