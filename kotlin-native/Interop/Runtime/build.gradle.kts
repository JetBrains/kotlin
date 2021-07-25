/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.gradle.plugins.tools.lib
import org.jetbrains.gradle.plugins.tools.solib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream

val kotlinVersion = project.bootstrapKotlinVersion
plugins {
    `native`
    `kotlin`
}
//apply plugin: 'c'




native {
    val isWindows = PlatformInfo.isWindows()
    val obj = if (isWindows) "obj" else "o"
    val host = rootProject.project(":kotlin-native").extra["hostName"]
    val hostLibffiDir = rootProject.project(":kotlin-native").extra["${host}LibffiDir"]
    val cflags = mutableListOf("-I$hostLibffiDir/include",
                               *platformManager.hostPlatform.clang.hostCompilerArgsForJni)
    suffixes {
        (".c" to ".$obj") {
            tool(*platformManager.hostPlatform.clang.clangC("").toTypedArray())
            flags( *cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "callbacks" {
            dir("src/callbacks/c")
        }
    }
    val objSet = sourceSets["callbacks"]!!.transform(".c" to ".$obj")

    target(solib("callbacks"), objSet) {
        tool(*platformManager.hostPlatform.clang.clangCXX("").toTypedArray())
        flags("-shared",
              "-o",ruleOut(), *ruleInAll(),
              "-L${project(":kotlin-native:libclangext").buildDir}",
              "$hostLibffiDir/lib/libffi.a",
              "-lclangext")
    }
    tasks.named(solib("callbacks")).configure {
        dependsOn(":kotlin-native:libclangext:${lib("clangext")}")
    }
}

dependencies {
    implementation(project(":kotlin-native:utilities:basic-utils"))
    implementation(project(":kotlin-stdlib"))
    implementation(project(":kotlin-reflect"))
}

sourceSets.main.get().java.srcDir("src/jvm/kotlin")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xskip-prerelease-check"
        )
        allWarningsAsErrors = true
    }
}


val nativelibs = project.tasks.create<Copy>("nativelibs") {
    val callbacksSolib = solib("callbacks")
    dependsOn(callbacksSolib)

    from("$buildDir/$callbacksSolib")
    into("$buildDir/nativelibs/")
}
