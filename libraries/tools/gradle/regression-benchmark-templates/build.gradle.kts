plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation(libs.okhttp)
    implementation(libs.jgit)
    implementation("org.slf4j:slf4j-nop:1.7.32")
    implementation("org.jetbrains.kotlinx:dataframe:0.14.1")
    implicitDependencies("org.jetbrains.kotlinx:dataframe:0.14.1") {
        because("workaround for KTIJ-30065, remove after its resolution")
    }
}
