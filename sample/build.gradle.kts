buildscript {
  dependencies {
    classpath("gradle.plugin.com.bnorm.power:kotlin-power-assert-gradle:0.1.0")
  }
}

plugins {
  kotlin("jvm") version "1.3.60"
}
apply(plugin = "com.bnorm.power.kotlin-power-assert")

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("test-junit"))
  testImplementation("org.assertj:assertj-core:3.15.0")
}

tasks.compileTestKotlin {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.useIR = true
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
  functions = listOf("kotlin.test.AssertionsKt.assertTrue", "kotlin.PreconditionsKt.require")
}
