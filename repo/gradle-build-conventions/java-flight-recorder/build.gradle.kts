plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}

dependencies {
    implementation(kotlinBuildHelpers())
}
