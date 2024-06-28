plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
}
