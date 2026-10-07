plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())
    implementation(project(":compiler:config"))
    api(project(":native:kotlin-native-utils"))
}
