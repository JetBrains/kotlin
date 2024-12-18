import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
}

description = "Kotlin/Native compiler"

val kotlinNative by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val shadowJar by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    kotlinNative(project(":kotlin-native:Interop:Skia"))
    kotlinNative(project(":kotlin-native:utilities:cli-runner"))
}

val compilerJar by tasks.registering(ShadowJar::class) {
    from(kotlinNative)
    mergeServiceFiles()
    // Exclude trove4j because of license agreement.
    exclude("*trove4j*")
    exclude("META-INF/versions/9/module-info.class")
}

artifacts {
    add(shadowJar.name, compilerJar)
}