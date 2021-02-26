# Kotlin/Native Gradle plugin

Since 1.3.40, a separate Gradle plugin for Kotlin/Native is deprecated in favor of the `kotlin-multiplatform` plugin.
This plugin provides an IDE support along with support of the new multiplatform project model introduced in Kotlin 1.3.0.
Below you can find a short list of differences between `kotlin-platform-native` and `kotlin-muliplatform` plugins. 
For more information see the `kotlin-muliplatform` [documentation page](https://kotlinlang.org/docs/mpp-discover-project.html).
For `kotlin-platform-native` reference see the [corresponding section](#kotlin-platform-native-reference).

### Applying the multiplatform plugin

To apply the `kotlin-multiplatform` plugin, just add the following snippet into your build script:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
plugins {
    id("org.jetbrains.kotlin.multiplatform") version '1.3.40'
}
```

</div>


### Managing targets

With the `kotlin-platform-native` plugin a set of target platforms is specified as a list in properties of the main component:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    targets = ['macos_x64', 'linux_x64', 'mingw_x64']
}
```

</div>

With the `kotlin-multiplatform` plugin target platforms can be added into a project using special methods available in the `kotlin` extension.
Each method adds into a project one __target__ which can be accessed using the `targets` property. Each target can be configured independently
including output kinds, additional compiler options etc. See details about targets at the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets).

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

kotlin {
    // These targets are declared without any target-specific settings. 
    macosX64()
    linuxX64()
    
    // You can specify a custom name used to access the target.
    mingwX64("windows") 
    
    iosArm64 {
        // Additional settings for ios_arm64.
    }
    
    // You can access declared targets using the `targets` property.
    println(targets.macosX64)
    println(targets.windows)
    
    // You also can configure all native targets in a single block.
    targets.withType(KotlinNativeTarget) {
        // Native target configuration.
    }
}
```

</div>

Each target includes two __compilations__: `main` and `test` compiling product and test sources respectively. A compilation is an abstraction
over a compiler invocation and described at the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#configuring-compilations).

### Managing sources

With the `kotlin-platform-native` plugin source sets are used to separate test and product sources. Also you can specify different sources
for different platforms in the same source set:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
sourceSets {
    // Adding target-independent sources.
    main.kotlin.srcDirs += 'src/main/mySources'
    
    // Adding Linux-specific code.
    main.target('linux_x64').srcDirs += 'src/main/linux'
}
```

</div> 

With the `kotlin-multiplatform` plugin __source__ __sets__ are also used to group sources but source files for different platforms are located in different source sets.
For each declared target two source sets are created: `<target-name>Main` and `<target-name>Test` containing product and test sources for this platform. Common for all
platforms sources are located in `commonMain` and `commonTest` source sets created by default. More information about source sets can be found
[here](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#configuring-source-sets).
 
<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
kotlin {
    sourceSets {
        // Adding target-independent sources.
        commonMain.kotlin.srcDirs += file("src/main/mySources")

        // Adding Linux-specific code.
        linuxX64Main.kotlin.srcDirs += file("src/main/linux")
    }
}
```

</div> 

### Managing dependencies

With the `kotlin-platform-native` plugin dependencies are configured in a traditional for Gradle way by grouping them into configurations
using the project `dependencies` block:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
dependencies {
    implementation 'org.sample.test:mylibrary:1.0'
    testImplementation 'org.sample.test:testlibrary:1.0'
}
```

</div>

The `kotlin-multiplatform` plugin also uses configurations under the hood but it also provides a `dependencies` block for each source set
allowing configuring dependencies of this sources set:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
kotlin.sourceSets {
    commonMain {
        dependencies {
            implementation("org.sample.test:mylibrary:1.0")
        }
    }
    
    commonTest {
        dependencies {
            implementation("org.sample.test:testlibrary:1.0")
        }
    }
}
```

</div>

Note that a module referenced by a dependency declared for `commonMain` or `commonTest` source set must be published using the `kotlin-multiplatform` plugin.
If you want to use libraries published by the `kotlin-platform-native` plugin, you need to declare a separate source set for common native sources.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
kotlin.sourceSets {
    // Create a common source set used by native targets only.
    nativeMain {
        dependsOn(commonMain)
        dependencies {
            // Depend on a library published by the kotlin-platform-naive plugin.
            implementation("org.sample.test:mylibrary:1.0")
        }
    }

    // Configure all native platform sources sets to use it as a common one.
    linuxX64Main.dependsOn(nativeMain)
    macosX64Main.dependsOn(nativeMain)
    // ...
}
```

</div>

See more info about dependencies at the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#adding-dependencies).

### Output kinds

With the `kotlin-platform-native` plugin output kinds are specified as a list in properties of a component:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    // Compile the component into an executable and a Kotlin/Native library.
    outputKinds = [EXECUTABLE, KLIBRARY]
}
```

</div> 

With the `kotlin-multiplatform` plugin a compilation always produces a `*.klib` file. A separate `binaries` block is used to configure what
final native binaries should be produced by each target. Each binary can be configured independently including linker options, executable entry point etc.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
kotlin {
    macosX64 {
        binaries {
            executable {
                // Binary configuration: linker options, name, etc.
            }
            framework {
                // ...
            }
            
        }
    }
}
```

</div> 

See more about native binaries declaration at the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries).


### Publishing

Both `kotlin-platform-native` and `kotlin-multiplatform` plugins automatically set up artifact publication when the
`maven-publish` plugin is applied. See details about publication at the [corresponding page](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#publishing-a-multiplatform-library).
Note that currently only Kotlin/Native libraries (`*.klib`) can be published for native targets.

### Cinterop support

With the `kotlin-platform-native` plugin interop with a native library can be declared in component dependencies:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    dependencies {
        cinterop('mystdio') {
            // Cinterop configuration.
        }
    }
}
```

</div>

With the `kotlin-multiplatform` plugin interops are configured as a part of a compilation (see details [here](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#cinterop-support)).
The rest of an interop configuration is the same as for the `kotlin-platform-native` plugin.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
kotlin {
    macosX64 {
        compilations.main.cinterops {
            mystdio {
                // Cinterop configuration.
            }
        }
    }
}
```

</div>

## `kotlin-platform-native` reference

### Overview

You may use the Gradle plugin to build _Kotlin/Native_ projects. Builds of the plugin are
[available](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.platform.native) at the Gradle plugin portal, so you can apply it
using Gradle plugin DSL:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
plugins {
    id "org.jetbrains.kotlin.platform.native" version "1.3.0-rc-146"
}
```

</div>

You also can get the plugin from a Bintray repository. In addition to releases, this repo contains old and development
versions of the plugin which are not available at the plugin portal. To get the plugin from the Bintray repo, include
the following snippet in your build script:
   
<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
buildscript {
   repositories {
       mavenCentral()
       maven {
           url "https://dl.bintray.com/jetbrains/kotlin-native-dependencies"
       }
   }

   dependencies {
       classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:1.3.0-rc-146"
   }
}

apply plugin: 'org.jetbrains.kotlin.platform.native'
```

</div>

By default the plugin downloads the Kotlin/Native compiler during the first run. If you have already downloaded the compiler
manually you can specify the path to its root directory using `org.jetbrains.kotlin.native.home` project property (e.g. in `gradle.properties`).

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
org.jetbrains.kotlin.native.home=/home/user/kotlin-native-0.8
```

</div>

In this case the compiler will not be downloaded by the plugin.

### Source management

Source management in the `kotlin.platform.native` plugin is uniform with other Kotlin plugins and is based on source sets.
A source set is a group of Kotlin/Native source which may contain both common and platform-specific code. The plugin
provides a top-level script block `sourceSets` allowing you to configure source sets. Also it creates the default
source sets `main` and `test` (for production and test code respectively).

By default the production sources are located in `src/main/kotlin` and the test sources - in `src/test/kotlin`.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
sourceSets {
    // Adding target-independent sources.
    main.kotlin.srcDirs += 'src/main/mySources'
    
    // Adding Linux-specific code. It will be compiled in Linux binaries only.
    main.target('linux_x64').srcDirs += 'src/main/linux'
}
```

</div> 

### Targets and output kinds

By default the plugin creates software components for the main and test source sets. You can access them via the
`components` container provided by Gradle or via the `component` property of a corresponding source set:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
// Main component.
components.main
sourceSets.main.component

// Test component.
components.test
sourceSets.test.component
```

</div> 

Components allow you to specify:

* Targets (e.g. Linux/x64 or iOS/arm64 etc)
* Output kinds (e.g. executable, library, framework etc)
* Dependencies (including interop ones)

Targets can be specified by setting a corresponding component property:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    // Compile this component for 64-bit MacOS, Linux and Windows.
    targets = ['macos_x64', 'linux_x64', 'mingw_x64']
}
```

</div> 

The plugin uses the same notation as the compiler. By default, test component uses the same targets as specified for the main one.

Output kinds can also be specified using a special property:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    // Compile the component into an executable and a Kotlin/Native library.
    outputKinds = [EXECUTABLE, KLIBRARY]
}
```

</div> 

All constants used here are available inside a component configuration script block.
The plugin supports producing binaries of the following kinds:

* `EXECUTABLE` - an executable file;
* `KLIBRARY` - a Kotlin/Native library (*.klib);
* `FRAMEWORK` - an Objective-C framework;
* `DYNAMIC` - shared native library;
* `STATIC` - static native library.

Also each native binary is built in two variants (build types): `debug` (debuggable, not optimized) and `release` (not debuggable, optimized).
Note that Kotlin/Native libraries have only `debug` variant because optimizations are preformed only during compilation
of a final binary (executable, static lib etc) and affect all libraries used to build it.

### Compile tasks

The plugin creates a compilation task for each combination of the target, output kind, and build type. The tasks have the following naming convention:

    compile<ComponentName><BuildType><OutputKind><Target>KotlinNative

For example `compileDebugKlibraryMacos_x64KotlinNative`, `compileTestDebugKotlinNative`.

The name contains the following parts (some of them may be empty):

* `<ComponentName>` - name of a component. Empty for the main component.
* `<BuildType>` - `Debug` or `Release`.
* `<OutputKind>` - output kind name, e.g. `Executabe` or `Dynamic`. Empty if the component has only one output kind.
* `<Target>` - target the component is built for, e.g. `Macos_x64` or `Wasm32`. Empty if the component is built only for one target.

Also the plugin creates a number of aggregate tasks allowing you to build all the binaries for a build type (e.g.
`assembleAllDebug`) or all the binaries for a particular target (e.g. `assembleAllWasm32`).

Basic lifecycle tasks like `assemble`, `build`, and `clean` are also available.

### Running tests

The plugin builds a test executable for all the targets specified for the `test` component. If the current host platform is
included in this list the test running tasks are also created. To run tests, execute the standard lifecycle `check` task:

<div class="sample" markdown="1" theme="idea" mode="shell">

```bash
./gradlew check
```

</div> 

### Dependencies

The plugin allows you to declare dependencies on files and other projects using traditional Gradle's mechanism of
configurations. The plugin supports Kotlin multiplatform projects allowing you to declare the `expectedBy` dependencies

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
dependencies {
    implementation files('path/to/file/dependencies')
    implementation project('library')
    testImplementation project('testLibrary')
    expectedBy project('common')
}
```

</div> 

It's possible to depend on a Kotlin/Native library published earlier in a maven repo. The plugin relies on Gradle's
[metadata](https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md)
support so the corresponding feature must be enabled. Add the following line in your `settings.gradle`:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
enableFeaturePreview('GRADLE_METADATA')
```

</div>


Now you can declare a dependency on a Kotlin/Native library in the traditional `group:artifact:version` notation:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
dependencies {
    implementation 'org.sample.test:mylibrary:1.0'
    testImplementation 'org.sample.test:testlibrary:1.0'
}
```

</div>

Dependency declaration is also possible in the component block:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    dependencies {
        implementation 'org.sample.test:mylibrary:1.0'
    }
}

components.test {
    dependencies {
        implementation 'org.sample.test:testlibrary:1.0'
    }
}
```

</div>


### Using cinterop

It's possible to declare a cinterop dependency for a component:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    dependencies {
        cinterop('mystdio') {
            // src/main/c_interop/mystdio.def is used as a def file.

            // Set up compiler options
            compilerOpts '-I/my/include/path'

            // It's possible to set up different options for different targets
            target('linux') {
                compilerOpts '-I/linux/include/path'
            }
        }
    }
}
```

</div>

Here an interop library will be built and added in the component dependencies.

Often it's necessary to specify target-specific linker options for a Kotlin/Native binary using an interop. It can be
done using the `target` script block:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    target('linux') {
        linkerOpts '-L/path/to/linux/libs'
    }
}
```

</div>

Also the `allTargets` block is available.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    // Configure all targets.
    allTargets {
        linkerOpts '-L/path/to/libs'
    }
}
```

</div>


### Publishing

In the presence of `maven-publish` plugin the publications for all the binaries built are created. The plugin uses Gradle
metadata to publish the artifacts so this feature must be enabled (see the [dependencies](#dependencies) section).

Now you can publish the artifacts with the standard Gradle `publish` task:

<div class="sample" markdown="1" theme="idea" mode="shell">

```bash
./gradlew publish
```

</div>
    
Only `EXECUTABLE` and `KLIBRARY` binaries are published currently.

The plugin allows you to customize the pom generated for the publication with the `pom` code block available for every component:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
components.main {
    pom {
        withXml {
            def root = asNode()
            root.appendNode('name', 'My library')
            root.appendNode('description', 'A Kotlin/Native library')
        }
    }
}
```

</div>

### Serialization plugin

The plugin is shipped with a customized version of the `kotlinx.serialization` plugin. To use it you don't have to 
add new buildscript dependencies, just apply the plugins and add a dependency on the serialization library:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
apply plugin: 'org.jetbrains.kotlin.platform.native'
apply plugin: 'kotlinx-serialization-native'

dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-runtime-native'
}
```

</div>

The [example project](https://github.com/ilmat192/kotlin-native-serialization-sample) for details.

### DSL example

In this section a commented DSL is shown. 
See also the example projects that use this plugin, e.g.
[Kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines),
[MPP http client](https://github.com/e5l/http-client-common/tree/master/samples/ios-test-application)

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
plugins {
    id "org.jetbrains.kotlin.platform.native" version "1.3.0-rc-146"
}

sourceSets.main {
    // Plugin uses Gradle's source directory sets here,
    // so all the DSL methods available in SourceDirectorySet can be called here.
    // Platform independent sources.
    kotlin.srcDirs += 'src/main/customDir'

    // Linux-specific sources
    target('linux').srcDirs += 'src/main/linux'
}

components.main {

    // Set up targets
    targets = ['linux_x64', 'macos_x64', 'mingw_x64']

    // Set up output kinds
    outputKinds = [EXECUTABLE, KLIBRARY, FRAMEWORK, DYNAMIC, STATIC]
    
    // Specify custom entry point for executables
    entryPoint = "org.test.myMain"

    // Target-specific options
    target('linux_x64') {
        linkerOpts '-L/linux/lib/path'
    }

    // Targets independent options
    allTargets {
        linkerOpts '-L/common/lib/path'
    }

    dependencies {

        // Dependency on a published Kotlin/Native library.
        implementation 'org.test:mylib:1.0'

        // Dependency on a project
        implementation project('library')

        // Cinterop dependency
        cinterop('interop-name') {
            // Def-file describing the native API.
            // The default path is src/main/c_interop/<interop-name>.def
            defFile project.file("deffile.def")

            // Package to place the Kotlin API generated.
            packageName 'org.sample'

            // Options to be passed to compiler and linker by cinterop tool.
            compilerOpts 'Options for native stubs compilation'
            linkerOpts 'Options for native stubs'

            // Additional headers to parse.
            headers project.files('header1.h', 'header2.h')

            // Directories to look for headers.
            includeDirs {
                // All objects accepted by the Project.file method may be used with both options.

                // Directories for header search (an analogue of the -I<path> compiler option).
                allHeaders 'path1', 'path2'

                // Additional directories to search headers listed in the 'headerFilter' def-file option.
                // -headerFilterAdditionalSearchPrefix command line option analogue.
                headerFilterOnly 'path1', 'path2'
            }
            // A shortcut for includeDirs.allHeaders.
            includeDirs "include/directory" "another/directory"

            // Pass additional command line options to the cinterop tool.
            extraOpts '-verbose'

            // Additional configuration for Linux.
            target('linux') {
                compilerOpts 'Linux-specific options'
            }
        }
    }

    // Additional pom settings for publication.
    pom {
        withXml {
            def root = asNode()
            root.appendNode('name', 'My library')
            root.appendNode('description', 'A Kotlin/Native library')
        }
    }

    // Additional options passed to the compiler.
    extraOpts '--time'
}
```

</div>
