import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
}

dependencies {
    implementation(project(":lib"))
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    languageVersion.set(KotlinVersion.KOTLIN_2_0)
}

application {
    mainClass.set("foo.MainKt")
}
