package org.jetbrains.dokka.Utilities

import java.io.File
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipEntry

data class ServiceDescriptor(val name: String, val category: String, val description: String?, val className: String)

class ServiceLookupException(message: String) : Exception(message)

object ServiceLocator {
    fun <T : Any> lookup(clazz: Class<T>, category: String, implementationName: String): T {
        val descriptor = lookupDescriptor(category, implementationName)
        return lookup(clazz, descriptor)
    }

    fun <T : Any> lookup(
        clazz: Class<T>,
        descriptor: ServiceDescriptor
    ): T {
        val loadedClass = javaClass.classLoader.loadClass(descriptor.className)
        val constructor = loadedClass.constructors.firstOrNull { it.parameterTypes.isEmpty() } ?: throw ServiceLookupException("Class ${descriptor.className} has no corresponding constructor")

        val implementationRawType: Any =
            if (constructor.parameterTypes.isEmpty()) constructor.newInstance() else constructor.newInstance(constructor)

        if (!clazz.isInstance(implementationRawType)) {
            throw ServiceLookupException("Class ${descriptor.className} is not a subtype of ${clazz.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return implementationRawType as T
    }

    private fun lookupDescriptor(category: String, implementationName: String): ServiceDescriptor {
        val properties = javaClass.classLoader.getResourceAsStream("dokka/$category/$implementationName.properties")?.use { stream ->
            Properties().let { properties ->
                properties.load(stream)
                properties
            }
        } ?: throw ServiceLookupException("No implementation with name $implementationName found in category $category")

        val className = properties["class"]?.toString() ?: throw ServiceLookupException("Implementation $implementationName has no class configured")

        return ServiceDescriptor(implementationName, category, properties["description"]?.toString(), className)
    }

    fun URL.toFile(): File {
        assert(protocol == "file")

        return try {
            File(toURI())
        } catch (e: URISyntaxException) { //Try to handle broken URLs, with unescaped spaces
            File(path)
        }
    }

    fun allServices(category: String): List<ServiceDescriptor> {
        val entries = this.javaClass.classLoader.getResources("dokka/$category")?.toList() ?: emptyList()

        return entries.flatMap {
            when (it.protocol) {
                "file" -> it.toFile().listFiles()?.filter { it.extension == "properties" }?.map { lookupDescriptor(category, it.nameWithoutExtension) } ?: emptyList()
                "jar" -> {
                    val file = JarFile(URL(it.file.substringBefore("!")).toFile())
                    try {
                        val jarPath = it.file.substringAfterLast("!").removePrefix("/").removeSuffix("/")
                        file.entries()
                                .asSequence()
                                .filter { entry -> !entry.isDirectory && entry.path == jarPath && entry.extension == "properties" }
                                .map { entry ->
                                    lookupDescriptor(category, entry.fileName.substringBeforeLast("."))
                                }.toList()
                    } finally {
                        file.close()
                    }
                }
                else -> emptyList<ServiceDescriptor>()
            }
        }
    }
}

inline fun <reified T : Any> ServiceLocator.lookup(category: String, implementationName: String): T = lookup(T::class.java, category, implementationName)
inline fun <reified T : Any> ServiceLocator.lookup(desc: ServiceDescriptor): T = lookup(T::class.java, desc)

private val ZipEntry.fileName: String
    get() = name.substringAfterLast("/", name)

private val ZipEntry.path: String
    get() = name.substringBeforeLast("/", "").removePrefix("/")

private val ZipEntry.extension: String?
    get() = fileName.let { fn -> if ("." in fn) fn.substringAfterLast(".") else null }
