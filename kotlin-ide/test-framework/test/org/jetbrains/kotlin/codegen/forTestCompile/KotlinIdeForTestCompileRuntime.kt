package org.jetbrains.kotlin.codegen.forTestCompile

import org.jetbrains.kotlin.idea.test.kotlinIdeRoot
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

    @JvmStatic
    fun runtimeJarForTests(): File {
        return assertExists(File("dependencies/repo/org/jetbrains/kotlin/kotlin-stdlib/1.4-SNAPSHOT/kotlin-stdlib-1.4-SNAPSHOT.jar"))
    }

    @JvmStatic
    fun runtimeJarForTestsWithJdk8(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar"))
    }

    @JvmStatic
    fun minimalRuntimeJarForTests(): File {
        return assertExists(File("dist/kotlin-stdlib-minimal-for-test.jar"))
    }

    @JvmStatic
    fun kotlinTestJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test.jar"))
    }

    @JvmStatic
    fun kotlinTestJUnitJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test-junit.jar"))
    }

    @JvmStatic
    fun kotlinTestJsJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-test-js.jar"))
    }

    @JvmStatic
    fun reflectJarForTests(): File {
        return assertExists(File("dependencies/repo/org/jetbrains/kotlin/kotlin-reflect/1.4-SNAPSHOT/kotlin-reflect-1.4-SNAPSHOT.jar"))
    }

    @JvmStatic
    fun scriptRuntimeJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-script-runtime.jar"))
    }

    @JvmStatic
    fun runtimeSourcesJarForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-sources.jar"))
    }

    @JvmStatic
    fun stdlibMavenSourcesJarForTests(): File {
        return assertExists(File("dist/maven/kotlin-stdlib-sources.jar"))
    }

    @JvmStatic
    fun stdlibCommonForTests(): File {
        return assertExists(File("dist/common/kotlin-stdlib-common.jar"))
    }

    @JvmStatic
    fun stdlibCommonSourcesForTests(): File {
        return assertExists(File("dist/common/kotlin-stdlib-common-sources.jar"))
    }

    @JvmStatic
    fun stdlibJsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-stdlib-js.jar"))
    }

    @JvmStatic
    fun jetbrainsAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/annotations-13.0.jar"))
    }

    @JvmStatic
    fun jvmAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-annotations-jvm.jar"))
    }

    @JvmStatic
    fun androidAnnotationsForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-annotations-android.jar"))
    }

    @JvmStatic
    fun coroutinesCompatForTests(): File {
        return assertExists(File("dist/kotlinc/lib/kotlin-coroutines-experimental-compat.jar"))
    }

    private fun assertExists(file: File): File {
        val absolute = File(kotlinIdeRoot).resolve(file)
        check(absolute.exists()) { "$file does not exist. Run 'gradlew dist'" }
        return absolute
    }

    @JvmStatic
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

    @JvmStatic
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