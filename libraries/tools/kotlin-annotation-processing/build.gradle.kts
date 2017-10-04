import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Annotation Processor wrapper for Kotlin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

apply { plugin("kotlin") }

val packedJars by configurations.creating

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-annotation-processing"))
    compileOnly("org.jetbrains.kotlin:gradle-api:1.6")
    testCompile("org.jetbrains.kotlin:gradle-api:1.6")
    compileOnly("com.android.tools.build:gradle:1.1.0")
    testCompile("com.android.tools.build:gradle:1.1.0")
    testCompile(commonDep("junit:junit"))
    packedJars(project(":kotlin-annotation-processing")) { isTransitive = false }
}

projectTest {
    workingDir = projectDir
}

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(packedJars)
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
}
sourcesJar()
javadocJar()

publish()
