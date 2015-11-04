package org.jetbrains.dokka.Utilities

import java.io.File
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipEntry

data class ServiceDescriptor(val name: String, val category: String, val description: String?, val className: String)

class ServiceLookupException(message: String) : Exception(message)

public object ServiceLocator {
    public fun <T : Any> lookup(clazz: Class<T>, category: String, implementationName: String): T {
        val descriptor = lookupDescriptor(category, implementationName)
        val loadedClass = javaClass.classLoader.loadClass(descriptor.className)
        val constructor = loadedClass.constructors
                .filter { it.parameterTypes.isEmpty() }
                .firstOrNull() ?: throw ServiceLookupException("Class ${descriptor.className} has no corresponding constructor")

        val implementationRawType: Any = if (constructor.parameterTypes.isEmpty()) constructor.newInstance() else constructor.newInstance(constructor)

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

    fun allServices(category: String): List<ServiceDescriptor> = javaClass.classLoader.getResourceAsStream("dokka/$category")?.use { stream ->
        val entries = this.javaClass.classLoader.getResources("dokka/$category")?.toList() ?: emptyList()

        entries.flatMap {
            when (it.protocol) {
                "file" -> File(it.file).listFiles()?.filter { it.extension == "properties" }?.map { lookupDescriptor(category, it.nameWithoutExtension) } ?: emptyList()
                "jar" -> {
                    val file = JarFile(it.file.removePrefix("file:").substringBefore("!"))
                    try {
                        val jarPath = it.file.substringAfterLast("!").removePrefix("/")
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
    } ?: emptyList()
}

public inline fun <reified T : Any> ServiceLocator.lookup(category: String, implementationName: String): T = lookup(T::class.java, category, implementationName)

private val ZipEntry.fileName: String
    get() = name.substringAfterLast("/", name)

private val ZipEntry.path: String
    get() = name.substringBeforeLast("/", "").removePrefix("/")

private val ZipEntry.extension: String?
    get() = fileName.let { fn -> if ("." in fn) fn.substringAfterLast(".") else null }
