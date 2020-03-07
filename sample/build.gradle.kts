buildscript {
  dependencies {
    classpath("gradle.plugin.com.bnorm.power:kotlin-power-assert-gradle:0.2.0")
  }
}

plugins {
  kotlin("jvm") version "1.3.70"
}
apply(plugin = "com.bnorm.power.kotlin-power-assert")

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("test-junit"))
}

tasks.compileTestKotlin {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.useIR = true
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
  functions = listOf("kotlin.test.assertTrue", "kotlin.require")
}
