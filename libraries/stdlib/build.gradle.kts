
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
        java.setSrcDirs(listOf(File(projectDir,"src")))
    }
    sourceSets.getByName("test").apply {
        java.setSrcDirs(listOf(File(projectDir,"test")))
    }
}

dependencies {
    compile(project(":core.builtins"))
}

task("sourcesets") {
    doLast {
        the<JavaPluginConvention>().sourceSets.all {
            println("--> ${it.name}: ${it.java.srcDirs.joinToString()}")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.moduleName = "kotlin-stdlib"
    kotlinOptions.allowKotlinPackage = true
    kotlinOptions.inheritMultifileParts = true
    kotlinOptions.declarationsOutputPath = File(buildDir, "declarations/stdlib-declarations.json").canonicalPath
}

