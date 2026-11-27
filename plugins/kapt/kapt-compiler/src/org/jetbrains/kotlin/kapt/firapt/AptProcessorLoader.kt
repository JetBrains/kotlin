/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.kapt.base.KaptFlag
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.util.AbstractMap
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.annotation.processing.Processor
import javax.management.ObjectName

/**
 * Loads the processors from a class loader that is reused across builds in the same JVM, like the kapt task's class loader cache
 * (`kapt.classloaders.cache.size`), so that the processors' code stays warm.
 *
 * The compiler loads this plugin in a new class loader for each compilation, and the Gradle daemon restores the system properties
 * after each build, so the cache is kept in the platform MBean server, which outlives both. A new processor class loader in each
 * build would also leak: `java.beans` keeps the classes it introspects (MapStruct's FreeMarker introspects its model classes)
 * behind soft references, and a class loader keeps the plugin's class loader through the access control context of its creation.
 */
internal object AptProcessorLoader {
    private val CACHE_NAME = ObjectName("org.jetbrains.kotlin.kapt:type=FirAptProcessorClassLoaders")
    private const val CACHE_SIZE = 8
    private const val SERVICE_FILE = "META-INF/services/javax.annotation.processing.Processor"

    @Suppress("UNCHECKED_CAST")
    private val cache: ConcurrentHashMap<String, AbstractMap.SimpleImmutableEntry<URLClassLoader, List<String>>>
        get() {
            val server = ManagementFactory.getPlatformMBeanServer()
            synchronized(server) {
                if (!server.isRegistered(CACHE_NAME)) {
                    server.registerMBean(ProcessorClassLoaders(ConcurrentHashMap<String, Any>()), CACHE_NAME)
                }
            }
            return server.getAttribute(CACHE_NAME, "Cache") as ConcurrentHashMap<String, AbstractMap.SimpleImmutableEntry<URLClassLoader, List<String>>>
        }

    fun load(options: KaptOptions, logger: KaptLogger): List<Processor> {
        val classpath = LinkedHashSet<File>(options.processingClasspath).apply {
            if (options[KaptFlag.INCLUDE_COMPILE_CLASSPATH]) addAll(options.compileClasspath)
        }.toList()
        val key = classpath.joinToString(File.pathSeparator) { "${it.absolutePath}@${it.lastModified()}" }
        val cache = cache
        val entry = cache.getOrPut(key) {
            if (cache.size >= CACHE_SIZE) cache.keys.firstOrNull()?.let { cache.remove(it)?.key?.close() }
            AbstractMap.SimpleImmutableEntry(
                URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()),
                discoverProcessorNames(classpath),
            )
        }
        val classLoader = entry.key
        val names = options.processors.ifEmpty { entry.value }
        return names.mapNotNull { name ->
            try {
                Class.forName(name, true, classLoader).getDeclaredConstructor().newInstance() as Processor
            } catch (e: Throwable) {
                logger.warn("Can't load annotation processor $name: $e")
                null
            }
        }
    }

    private fun discoverProcessorNames(classpath: List<File>): List<String> {
        val result = LinkedHashSet<String>()
        fun add(text: String) = text.lineSequence().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.forEach { result += it }
        for (file in classpath) {
            when {
                file.isDirectory -> file.resolve(SERVICE_FILE).takeIf { it.isFile }?.let { add(it.readText()) }
                file.isFile && file.extension.equals("jar", ignoreCase = true) -> ZipFile(file).use { zip ->
                    zip.getEntry(SERVICE_FILE)?.let { entry -> add(zip.getInputStream(entry).bufferedReader().readText()) }
                }
            }
        }
        return result.toList()
    }
}

/** The MBean that holds the cache of [AptProcessorLoader]. The class loader of the build that registered it stays loaded. */
internal interface ProcessorClassLoadersMBean {
    val cache: Any
}

internal class ProcessorClassLoaders(override val cache: Any) : ProcessorClassLoadersMBean
