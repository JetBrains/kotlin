
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.Project
import org.gradle.api.Task
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

fun Project.fixKotlinTaskDependencies() {
    the<JavaPluginConvention>().sourceSets.all { sourceset ->
        val taskName = if (sourceset.name == "main") "classes" else (sourceset.name + "Classes")
        tasks.withType<Task> {
            if (name == taskName) {
                dependsOn("copy${sourceset.name.capitalize()}KotlinClasses")
            }
        }
    }
}

// TODO: common ^ 8< ----

configure<JavaPluginConvention> {
    sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf(File(projectDir, "src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(emptyList<File>())//listOf(File(projectDir,"test")))
    }
}

val builtinsProjectName = ":core.builtins"

dependencies {
    compile(project(builtinsProjectName))
}

tasks.withType<JavaCompile> {
    dependsOn("$builtinsProjectName:assemble")
}

tasks.withType<KotlinCompile> {
    dependsOn("$builtinsProjectName:assemble")
    kotlinOptions.freeCompilerArgs =
            listOf("-Xallow-kotlin-package",
                    "-module-name", "kotlin-stdlib",
                    "-Xmultifile-parts-inherit",
                    "-Xdump-declarations-to", File(buildDir, "declarations/stdlib-declarations.json").canonicalPath)
}

fixKotlinTaskDependencies()
