plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("org.slf4j:slf4j-nop:1.7.32")
    // Dev version contains the fix for this bug: https://github.com/Kotlin/dataframe/issues/101
    implementation("org.jetbrains.kotlinx:dataframe:0.8.0-dev-952")
}
