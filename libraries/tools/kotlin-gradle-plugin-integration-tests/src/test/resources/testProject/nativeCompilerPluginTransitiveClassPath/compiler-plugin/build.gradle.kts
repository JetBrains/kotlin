plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":helper"))

    compileOnly(kotlin("compiler-embeddable"))
}
