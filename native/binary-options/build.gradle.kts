plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())
    implementation(project(":compiler:config"))
}
