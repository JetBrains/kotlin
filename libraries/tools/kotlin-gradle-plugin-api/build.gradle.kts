import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    kotlin("jvm")
    maven
    id("jps-compatible")
}

publish()

standardPublicJars()

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-native:kotlin-native-utils"))

    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:0.4.2")
}

pill {
    variant = PillExtension.Variant.FULL
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.languageVersion = "1.2"
        kotlinOptions.apiVersion = "1.2"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    named<Jar>("jar") {
        callGroovy("manifestAttributes", manifest, project)
    }
}