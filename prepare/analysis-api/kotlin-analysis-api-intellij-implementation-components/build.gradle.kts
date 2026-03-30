plugins {
    `java-library`
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-intellij-api-surface-components"))

    implementation(libs.analysis.api.kotlin.reflect)
    implementation(libs.org.jetbrains.annotations)
    implementation(libs.kotlinx.serialization.json)
    implementation(commonDependency("javax.inject"))
    implementation(commonDependency("org.jline", "jline"))
    implementation(commonDependency("org.fusesource.jansi", "jansi"))
    implementation(commonDependency("com.google.code.findbugs", "jsr305"))
    implementation(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
    implementation(commonDependency("org.lz4:lz4-java"))
    implementation(commonDependency("com.fasterxml:aalto-xml"))
    implementation(commonDependency("org.codehaus.woodstox:stax2-api"))
    implementation(commonDependency("oro:oro"))
    implementation(commonDependency("one.util:streamex"))
    implementation(libs.vavr)
    implementation(libs.guava)
    implementation(libs.auto.value.annotations)

    embedded(project(":dependencies:intellij-core-implementation"))
    embedded(intellijUtilRtJava8()) { isTransitive = false }

    embedded(commonDependency("org.jetbrains.intellij.deps.jna:jna")) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.intellij.deps.jna:jna-platform")) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.intellij.deps:log4j")) { isTransitive = false }
    embedded(intellijJDom()) { isTransitive = false }
    embedded(libs.analysis.api.intellij.patched.kotlinx.coroutines.core.jvm) { isTransitive = false }
    embedded(libs.intellij.fastutil)
    embedded(libs.intellij.asm)
}

publishAnalysisApiArtifact()