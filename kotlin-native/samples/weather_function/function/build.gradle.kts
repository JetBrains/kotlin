group = "org.example"
version = "0.1-SNAPSHOT"

plugins {
    id("konan")
}

konanArtifacts {
    interop("curl") {
        defFile("curl.def")
    }

    interop("cjson") {
        defFile("cjson.def")
    }

    program("weather") {
        entryPoint("org.example.weather_func.main")
        libraries {
            artifact("cjson")
            artifact("curl")
        }
    }
}
