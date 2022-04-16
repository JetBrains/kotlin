plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
}

dependencies {
    api(project(":native:kotlin-native-utils"))
    api(project(":kotlin-project-model"))
    implementation(project(":kotlin-tooling-core"))
    compileOnly("com.android.tools.build:gradle:3.4.0")
}
