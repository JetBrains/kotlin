plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(libs.intellij.asm)
    implementation(commonDependency("org.apache.commons:commons-text"))

    implementation(project(":tools:kotlinp-jvm"))
    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-metadata"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    constraints {
        api(libs.apache.commons.lang)
    }
}

runtimeJar {
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.abicmp.AbiComparatorMain"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
             configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
         })
}

application {
    mainClass = "org.jetbrains.kotlin.abicmp.AbiComparatorMain"
}
