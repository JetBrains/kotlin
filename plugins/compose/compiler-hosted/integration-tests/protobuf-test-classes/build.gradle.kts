plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("com.google.protobuf") version "0.9.6"
    id("java")
}

dependencies {
    implementation(libs.protobuf.java.compose.test)
}

protobuf {
    protoc {
        val protocVersion = libs.versions.protobufComposeTest.get()
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
