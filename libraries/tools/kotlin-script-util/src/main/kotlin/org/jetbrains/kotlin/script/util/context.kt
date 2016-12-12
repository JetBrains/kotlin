package org.jetbrains.kotlin.script.util

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Manifest
import kotlin.reflect.KClass

private fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        }
        catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

fun classpathFromClassloader(classLoader: ClassLoader): List<File>? =
        generateSequence(classLoader) { it.parent }.toList().flatMap { (it as? URLClassLoader)?.urLs?.mapNotNull(URL::toFile) ?: emptyList() }

fun classpathFromClasspathProperty(): List<File>? =
        System.getProperty("java.class.path")
                ?.split(String.format("\\%s", File.pathSeparatorChar).toRegex())
                ?.dropLastWhile(String::isEmpty)
                ?.map(::File)

fun classpathFromClass(classLoader: ClassLoader, klass: KClass<out Any>): List<File>? {
    val clp = "${klass.qualifiedName?.replace('.', '/')}.class"
    val url = classLoader.getResource(clp)
    return url?.toURI()?.path?.removeSuffix(clp)?.let {
        listOf(File(it))
    }
}

// Maven runners sometimes place classpath into the manifest, so we can use it for a fallback search
fun manifestClassPath(classLoader: ClassLoader): List<File>? =
        classLoader.getResources("META-INF/MANIFEST.MF")
                .asSequence()
                .mapNotNull { ifFailed(null) { it.openStream().use { Manifest().apply { read(it) } } } }
                .flatMap { it.mainAttributes?.getValue("Class-Path")?.splitToSequence(" ") ?: emptySequence() }
                .mapNotNull { ifFailed(null) { File(URI.create(it)) } }
                .toList()
                .let { if (it.isNotEmpty()) it else null }

private inline fun <R> ifFailed(default: R, block: () -> R) = try {
    block()
} catch (t: Throwable) {
    default
}

