plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())
    implementation(project(":compiler:config"))
}
