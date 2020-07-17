package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


interface DokkaContext {
    fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T?

    operator fun <T, E> get(point: E): List<T>
            where T : Any, E : ExtensionPoint<T>

    fun <T, E> single(point: E): T where T : Any, E : ExtensionPoint<T>

    val logger: DokkaLogger
    val configuration: DokkaConfiguration
    val unusedPoints: Collection<ExtensionPoint<*>>


    companion object {
        fun create(
            configuration: DokkaConfiguration,
            logger: DokkaLogger,
            pluginOverrides: List<DokkaPlugin>
        ): DokkaContext =
            DokkaContextConfigurationImpl(logger, configuration).apply {
                // File(it.path) is a workaround for an incorrect filesystem in a File instance returned by Gradle.
                configuration.pluginsClasspath.map { File(it.path).toURI().toURL() }
                    .toTypedArray()
                    .let { URLClassLoader(it, this.javaClass.classLoader) }
                    .also { checkClasspath(it) }
                    .let { ServiceLoader.load(DokkaPlugin::class.java, it) }
                    .let { it + pluginOverrides }
                    .forEach { install(it) }
                topologicallySortAndPrune()
            }.also { it.logInitialisationInfo() }
    }
}

inline fun <reified T : DokkaPlugin> DokkaContext.plugin(): T = plugin(T::class)
    ?: throw java.lang.IllegalStateException("Plugin ${T::class.qualifiedName} is not present in context.")

interface DokkaContextConfiguration {
    fun installExtension(extension: Extension<*, *, *>)
}

private class DokkaContextConfigurationImpl(
    override val logger: DokkaLogger,
    override val configuration: DokkaConfiguration
) : DokkaContext, DokkaContextConfiguration {
    private val plugins = mutableMapOf<KClass<*>, DokkaPlugin>()
    private val pluginStubs = mutableMapOf<KClass<*>, DokkaPlugin>()
    val extensions = mutableMapOf<ExtensionPoint<*>, MutableList<Extension<*, *, *>>>()
    val pointsUsed: MutableSet<ExtensionPoint<*>> = mutableSetOf()
    val pointsPopulated: MutableSet<ExtensionPoint<*>> = mutableSetOf()
    override val unusedPoints: Set<ExtensionPoint<*>>
        get() = pointsPopulated - pointsUsed

    private enum class State {
        UNVISITED,
        VISITING,
        VISITED;
    }

    private sealed class Suppression {
        data class ByExtension(val extension: Extension<*, *, *>) : Suppression() {
            override fun toString() = extension.toString()
        }

        data class ByPlugin(val plugin: DokkaPlugin) : Suppression() {
            override fun toString() = "Plugin ${plugin::class.qualifiedName}"
        }
    }

    private val rawExtensions = mutableListOf<Extension<*, *, *>>()
    private val rawAdjacencyList = mutableMapOf<Extension<*, *, *>, MutableList<Extension<*, *, *>>>()
    private val suppressedExtensions = mutableMapOf<Extension<*, *, *>, MutableList<Suppression>>()

    fun topologicallySortAndPrune() {
        pointsPopulated.clear()
        extensions.clear()

        val overridesInfo = processOverrides()
        val extensionsToSort = overridesInfo.keys
        val adjacencyList = translateAdjacencyList(overridesInfo)

        val verticesWithState = extensionsToSort.associateWithTo(mutableMapOf()) { State.UNVISITED }
        val result: MutableList<Extension<*, *, *>> = mutableListOf()

        fun visit(n: Extension<*, *, *>) {
            val state = verticesWithState[n]
            if (state == State.VISITED)
                return
            if (state == State.VISITING)
                throw Error("Detected cycle in plugins graph")
            verticesWithState[n] = State.VISITING
            adjacencyList[n]?.forEach { visit(it) }
            verticesWithState[n] = State.VISITED
            result += n
        }

        extensionsToSort.forEach(::visit)

        val filteredResult = result.asReversed().filterNot { it in suppressedExtensions }

        filteredResult.mapTo(pointsPopulated) { it.extensionPoint }
        filteredResult.groupByTo(extensions) { it.extensionPoint }
    }

    private fun processOverrides(): Map<Extension<*, *, *>, Set<Extension<*, *, *>>> {
        val buckets = rawExtensions.associateWithTo(mutableMapOf()) { setOf(it) }
        suppressedExtensions.forEach { (extension, suppressions) ->
            val mergedBucket = suppressions.filterIsInstance<Suppression.ByExtension>()
                .map { it.extension }
                .plus(extension)
                .flatMap { buckets[it].orEmpty() }
                .toSet()
            mergedBucket.forEach { buckets[it] = mergedBucket }
        }
        return buckets.values.distinct().associateBy(::findNotOverridden)
    }

    private fun findNotOverridden(bucket: Set<Extension<*, *, *>>): Extension<*, *, *> {
        val filtered = bucket.filter { it !in suppressedExtensions }
        return filtered.singleOrNull() ?: throw IllegalStateException("Conflicting overrides: $filtered")
    }

    private fun translateAdjacencyList(
        overridesInfo: Map<Extension<*, *, *>, Set<Extension<*, *, *>>>
    ): Map<Extension<*, *, *>, List<Extension<*, *, *>>> {
        val reverseOverrideInfo = overridesInfo.flatMap { (ext, set) -> set.map { it to ext } }.toMap()
        return rawAdjacencyList.mapNotNull { (ext, list) ->
            reverseOverrideInfo[ext]?.to(list.mapNotNull { reverseOverrideInfo[it] })
        }.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T, E> get(point: E) where T : Any, E : ExtensionPoint<T> =
        actions(point).also { pointsUsed += point }.orEmpty() as List<T>

    @Suppress("UNCHECKED_CAST")
    override fun <T, E> single(point: E): T where T : Any, E : ExtensionPoint<T> {
        fun throwBadArity(substitution: String): Nothing = throw IllegalStateException(
            "$point was expected to have exactly one extension registered, but $substitution found."
        )
        pointsUsed += point

        val extensions = extensions[point].orEmpty() as List<Extension<T, *, *>>
        return when (extensions.size) {
            0 -> throwBadArity("none was")
            1 -> extensions.single().action.get(this)
            else -> throwBadArity("many were")
        }
    }

    private fun <E : ExtensionPoint<*>> actions(point: E) = extensions[point]?.map { it.action.get(this) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>) = (plugins[kclass] ?: pluginStubFor(kclass)) as T

    private fun <T : DokkaPlugin> pluginStubFor(kclass: KClass<T>): DokkaPlugin =
        pluginStubs.getOrPut(kclass) { kclass.createInstance().also { it.context = this } }

    fun install(plugin: DokkaPlugin) {
        plugins[plugin::class] = plugin
        plugin.context = this
        plugin.internalInstall(this, this.configuration)

        if (plugin is WithUnsafeExtensionSuppression) {
            plugin.extensionsSuppressed.forEach {
                suppressedExtensions.listFor(it) += Suppression.ByPlugin(plugin)
            }
        }
    }

    override fun installExtension(extension: Extension<*, *, *>) {
        rawExtensions += extension

        if (extension.ordering is OrderingKind.ByDsl) {
            val orderDsl = OrderDsl()
            orderDsl.(extension.ordering.block)()

            rawAdjacencyList.listFor(extension) += orderDsl.following.toList()
            orderDsl.previous.forEach { rawAdjacencyList.listFor(it) += extension }
        }

        if (extension.override is OverrideKind.Present) {
            suppressedExtensions.listFor(extension.override.overriden) += Suppression.ByExtension(extension)
        }
    }

    fun logInitialisationInfo() {
        val pluginNames = plugins.values.map { it::class.qualifiedName.toString() }

        val loadedListForDebug = extensions.run { keys + values.flatten() }.toList()
            .joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") { "\t$it" }

        val suppressedList = suppressedExtensions.asSequence()
            .joinToString(prefix = "[\n", separator = ",\n", postfix = "\n]") {
                "\t${it.key} by " + (it.value.singleOrNull() ?: it.value)
            }

        logger.info("Loaded plugins: $pluginNames")
        logger.info("Loaded: $loadedListForDebug")
        logger.info("Suppressed: $suppressedList")
    }
}

private fun checkClasspath(classLoader: URLClassLoader) {
    classLoader.findResource(DokkaContext::class.java.name.replace('.', '/') + ".class")?.also {
        throw AssertionError(
            "Dokka API found on plugins classpath. This will lead to subtle bugs. " +
                    "Please fix your plugins dependencies or exclude dokka api artifact from plugin classpath"
        )
    }
}

private fun <K, V> MutableMap<K, MutableList<V>>.listFor(key: K) = getOrPut(key, ::mutableListOf)
