import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

val disambiguationAttribute = Attribute.of("disambiguationAttribute", String::class.java)

kotlin {
    jvm("plainJvm") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        attributes { attribute(disambiguationAttribute, "plainJvm") }
    }

    jvm("jvmWithJava") {
        withJava()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        attributes { attribute(disambiguationAttribute, "jvmWithJava") }
    }
}
