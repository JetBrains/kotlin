package org.jetbrains.kotlin.idea.test

import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.lang.ref.SoftReference
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.*

object KotlinIdeForTestCompileRuntime {
    @Volatile
    private var reflectJarClassLoader = SoftReference<ClassLoader?>(null)

    @Volatile
    private var runtimeJarClassLoader = SoftReference<ClassLoader?>(null)

    fun runtimeJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib.jar"))
    }

    fun runtimeJarForTestsWithJdk8(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar"))
    }

    fun minimalRuntimeJarForTests(): File {
        return assertExists(File("dist/kotlin-stdlib-minimal-for-test.jar"))
    }

    fun kotlinTestJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test.jar"))
    }

    fun kotlinTestJUnitJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test-junit.jar"))
    }

    fun kotlinTestJsJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test-js.jar"))
    }

    fun reflectJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-reflect.jar"))
    }

    fun scriptRuntimeJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-script-runtime.jar"))
    }

    fun runtimeSourcesJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-sources.jar"))
    }

    fun stdlibMavenSourcesJarForTests(): File {
        return assertExists(File("dist/maven/kotlin-stdlib-sources.jar"))
    }

    fun stdlibCommonForTests(): File {
        return assertExists(File("dist/common/kotlin-stdlib-common.jar"))
    }

    fun stdlibCommonSourcesForTests(): File {
        return assertExists(File("dist/common/kotlin-stdlib-common-sources.jar"))
    }

    fun stdlibJsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-js.jar"))
    }

    fun jetbrainsAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/annotations-13.0.jar"))
    }

    fun jvmAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-annotations-jvm.jar"))
    }

    fun androidAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-annotations-android.jar"))
    }

    fun coroutinesCompatForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-coroutines-experimental-compat.jar"))
    }

    private fun assertExists(file: File): File {
        check(file.exists()) { "$file does not exist. Run 'gradlew dist'" }
        return file
    }

    @Synchronized
    fun runtimeAndReflectJarClassLoader(): ClassLoader {
        var loader = reflectJarClassLoader.get()
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(),
                                       reflectJarForTests(),
                                       scriptRuntimeJarForTests(),
                                       kotlinTestJarForTests())
            reflectJarClassLoader = SoftReference(loader)
        }
        return loader
    }

    @Synchronized
    fun runtimeJarClassLoader(): ClassLoader {
        var loader = runtimeJarClassLoader.get()
        if (loader == null) {
            loader = createClassLoader(runtimeJarForTests(),
                                       scriptRuntimeJarForTests(),
                                       kotlinTestJarForTests())
            runtimeJarClassLoader = SoftReference(loader)
        }
        return loader
    }

    private fun createClassLoader(vararg files: File): ClassLoader {
        return try {
            val urls: MutableList<URL> = ArrayList(2)
            for (file in files) {
                urls.add(file.toURI().toURL())
            }
            URLClassLoader(urls.toTypedArray(), null)
        }
        catch (e: MalformedURLException) {
            throw rethrow(e)
        }
    }
}