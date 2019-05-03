import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer.COMPILE

plugins {
    maven
    kotlin("jvm")
    id("jps-compatible")
}

val mavenCompileScope by configurations.creating {
    the<MavenPluginConvention>()
        .conf2ScopeMappings
        .addMapping(0, this, COMPILE)
}

description = "Kotlin/Native deserializer and library reader"

dependencies {

    // Compile-only dependencies are needed for compilation of this module:
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:cli-common"))

    // This dependency is necessary to keep the right dependency record inside of POM file:
    mavenCompileScope(projectRuntimeJar(":kotlin-compiler"))

    compile(project(":kotlin-native:kotlin-native-utils"))

    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()
