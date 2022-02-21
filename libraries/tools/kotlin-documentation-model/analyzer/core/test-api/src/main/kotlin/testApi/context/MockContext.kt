package org.jetbrains.dokka.testApi.context

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST") // It is only usable from tests so we do not care about safety
class MockContext(
    vararg extensions: Pair<ExtensionPoint<*>, (DokkaContext) -> Any>,
    private val testConfiguration: DokkaConfiguration? = null,
    private val unusedExtensionPoints: List<ExtensionPoint<*>>? = null
) : DokkaContext {
    private val extensionMap by lazy {
        extensions.groupBy(Pair<ExtensionPoint<*>, (DokkaContext) -> Any>::first) {
            it.second(this)
        }
    }

    private val plugins = mutableMapOf<KClass<out DokkaPlugin>, DokkaPlugin>()

    override fun <T : DokkaPlugin> plugin(kclass: KClass<T>): T = plugins.getOrPut(kclass) {
        kclass.constructors.single { it.parameters.isEmpty() }.call().also { it.injectContext(this) }
    } as T

    override fun <T : Any, E : ExtensionPoint<T>> get(point: E): List<T> = extensionMap[point].orEmpty() as List<T>

    override fun <T : Any, E : ExtensionPoint<T>> single(point: E): T = get(point).single()

    override val logger = DokkaConsoleLogger(LoggingLevel.DEBUG)

    override val configuration: DokkaConfiguration
        get() = testConfiguration ?: throw IllegalStateException("This mock context doesn't provide configuration")

    override val unusedPoints: Collection<ExtensionPoint<*>>
        get() = unusedExtensionPoints
            ?: throw IllegalStateException("This mock context doesn't provide unused extension points")
}

private fun DokkaPlugin.injectContext(context: DokkaContext) {
    (DokkaPlugin::class.memberProperties.single { it.name == "context" } as KMutableProperty<*>)
        .setter.call(this, context)
}
