plugins {
    kotlin("jvm")
}

val copyrightDirectory = project.layout.buildDirectory.dir("copyright")

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion")
    api("org.jetbrains.kotlin:kotlin-reflect:$bootstrapKotlinVersion")
}

val copyCopyrightProfile by tasks.registering(Copy::class) {
    from("$rootDir/.idea/copyright")
    into(copyrightDirectory)
    include("apache.xml")
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.addAll(listOf("-version", "-Xdont-warn-on-error-suppression"))
        }
    }

    register<JavaExec>("run") {
        group = "application"
        mainClass = "generators.GenerateStandardLibKt"
        classpath = sourceSets.main.get().runtimeClasspath
        args = listOf("$rootDir")
        systemProperty("line.separator", "\n")
    }

    register<JavaExec>("generateStdlibTests") {
        group = "application"
        mainClass = "generators.GenerateStandardLibTestsKt"
        classpath = sourceSets.main.get().runtimeClasspath
        args = listOf("$rootDir")
        systemProperty("line.separator", "\n")
    }

    register<JavaExec>("generateUnicodeData") {
        group = "application"
        mainClass = "generators.unicode.GenerateUnicodeDataKt"
        classpath = sourceSets.main.get().runtimeClasspath
        args = listOf("$rootDir")
    }
}

sourceSets {
    "main" {
        kotlin.srcDir("src")
        resources.srcDir(copyCopyrightProfile)
    }
}
