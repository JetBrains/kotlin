plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
}

dependencies {
    commonApi(project(":native:kotlin-native-utils"))
    commonApi(project(":kotlin-project-model"))
    commonImplementation(project(":kotlin-tooling-core"))
    commonCompileOnly("com.android.tools.build:gradle:3.6.4")
}
