import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    application
}

tasks.named<KotlinCompile>("compileKotlin").configure {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    implementation("org.test.example:lib:1.0.0")
}

application {
    mainClass.set("foo.MainKt")
}
