plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlinBuildHelpers())
    implementation(project(":utilities"))
    implementation(project(":java-flight-recorder"))
}
