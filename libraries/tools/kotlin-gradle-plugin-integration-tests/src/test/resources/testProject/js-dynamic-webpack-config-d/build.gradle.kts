plugins {
    kotlin("js")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        binaries.executable()
        browser()
    }
}

tasks.register("foo") {
    doLast {
        val dir = projectDir.resolve("webpack.config.d")
        dir.mkdirs()
        val file = dir.resolve("patch.js")
        file.createNewFile()
        file.writeText("// hello from patch.js")
    }
}

tasks.named("browserTest") {
    enabled = false
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack> {
    dependsOn("foo")
}
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile> {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
