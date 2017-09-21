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
    compileOnly(project(":kotlin-annotation-processing"))
    compile("org.jetbrains.kotlin:gradle-api:1.6")
    compile("com.android.tools.build:gradle:1.1.0")
    compile(projectDist(":kotlin-stdlib"))
    testCompile(commonDep("junit:junit"))
    packedJars(project(":kotlin-annotation-processing")) { isTransitive = false }
}

projectTest {
    workingDir = projectDir
}

val jar by tasks
jar.apply {
    enabled = false
}

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(packedJars)
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
}
sourcesJar()
javadocJar()

publish()
