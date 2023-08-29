plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation(libs.jgit)
    implementation("org.slf4j:slf4j-nop:1.7.32")
    implementation("org.jetbrains.kotlinx:dataframe:0.8.1")
}
