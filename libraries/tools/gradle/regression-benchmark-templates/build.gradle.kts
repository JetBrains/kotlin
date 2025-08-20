plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation(libs.okhttp)
    implementation(libs.jgit)
    implementation("org.slf4j:slf4j-nop:1.7.36")
    implementation(libs.dataframeCsv)
    implementation(libs.dataframeCore)
    implementation(libs.dataframeJson)
    implementation(libs.apache.commons.compress)
    implicitDependencies(libs.dataframeCsv) {
        because("workaround for KTIJ-30065, remove after its resolution")
    }
    implicitDependencies(libs.dataframeCore) {
        because("workaround for KTIJ-30065, remove after its resolution")
    }
    implicitDependencies(libs.dataframeJson) {
        because("workaround for KTIJ-30065, remove after its resolution")
    }
}
