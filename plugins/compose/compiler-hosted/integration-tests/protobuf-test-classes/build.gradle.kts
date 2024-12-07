plugins {
    id("com.google.protobuf") version "0.9.4"
    id("java")
}

dependencies {
    implementation(libs.protobuf.java.lite)
}

protobuf {
    protoc {
        val protocVersion = libs.versions.protobuf.get()
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }

    generateProtoTasks.all().configureEach {
        builtins {
            val java by getting {
                option("lite")
            }
        }
    }
}
