plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("com.google.protobuf") version "0.9.6"
    id("java")
}

val protocVersion = libs.versions.protobufComposeTest.get()

dependencies {
    implementation(libs.protobuf.java.compose.test)

    implicitDependencies("com.google.protobuf:protoc:$protocVersion:linux-x86_64@exe")
    implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-aarch_64@exe")
    implicitDependencies("com.google.protobuf:protoc:$protocVersion:osx-x86_64@exe")
    implicitDependencies("com.google.protobuf:protoc:$protocVersion:windows-x86_64@exe")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }

    generateProtoTasks.all().configureEach {
        builtins {
            val java = getByName("java") {
                option("lite")
            }
        }
    }
}

registerInAggregateGenerateSources("generateProto")
