import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "kotlinp"

plugins {
    kotlin("jvm")
}

val shadows by configurations.creating
shadows.extendsFrom(configurations.getByName("compile"))

dependencies {
    compile(project(":kotlinx-metadata"))
    compile(project(":kotlinx-metadata-jvm"))
    // We use an older version of ASM on purpose, to reduce the final application size
    compile("org.ow2.asm:asm:4.0")
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.kotlinp.test.GenerateKotlinpTestsKt")

val shadowJar by task<ShadowJar> {
    classifier = "shadow"
    version = null
    configurations = listOf(shadows)
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    manifest {
        attributes["Main-Class"] = "org.jetbrains.kotlin.kotlinp.Main"
    }
}
