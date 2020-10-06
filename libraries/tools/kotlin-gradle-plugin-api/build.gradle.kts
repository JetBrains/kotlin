import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()

standardPublicJars()

dependencies {
    compile(kotlinStdlib())

    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:0.4.2")
}

pill {
    variant = PillExtension.Variant.FULL
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check")
    }

    named<Jar>("jar") {
        callGroovy("manifestAttributes", manifest, project)
    }
}
