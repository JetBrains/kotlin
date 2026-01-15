plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/kt/dev")
}

val kotlin_version by properties
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}
