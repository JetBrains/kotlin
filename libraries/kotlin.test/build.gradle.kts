
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(
                listOf("shared/src/main/kotlin",
                        "shared/src/main/kotlin.jvm",
                        "junit/src/main/kotlin")
                        .map { File(projectDir, it) }
        )
        java.setSrcDirs(emptyList<File>())//listOf(File(projectDir,"test")))
    }
}

dependencies {
    compile(project(":core.builtins"))
    compile("junit:junit:4.11")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.allowKotlinPackage = true
}

tasks.withType<Jar> {
    exclude("kotlin/internal/OnlyInputTypes*", "kotlin/internal/InlineOnly*", "kotlin/internal")
}
