package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

class ExtensionBuilderStart internal constructor(){
    fun <T: Any>  extensionPoint(ext: ExtensionPoint<T>): ProvidedExtension<T> = ProvidedExtension(ext)
}

class ProvidedExtension<T: Any> internal constructor(val ext: ExtensionPoint<T>){
    fun fromInstance(inst: T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromInstance(
            inst
        )
    )
    fun fromRecipe(recipe: (DokkaContext) -> T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromRecipe(recipe)
    )

    private val defaultName = "${ext.pointName}/in/${javaClass.simpleName}"

    private fun createBuilder(action: LazyEvaluated<T>) =
        ExtensionBuilder(defaultName, ext, action,
            OrderingKind.None,
            OverrideKind.None, emptyList())
}

data class ExtensionBuilder<T: Any> internal constructor(
    private val name: String,
    private val ext: ExtensionPoint<T>,
    private val action: LazyEvaluated<T>,
    private val ordering: OrderingKind = OrderingKind.None,
    private val override: OverrideKind = OverrideKind.None,
    private val conditions: List<(DokkaConfiguration) -> Boolean>
){
    fun build(): Extension<T, *, *>  = Extension(
        ext,
        javaClass.name,
        name,
        action,
        ordering,
        override,
        conditions
    )

    fun overrideExtension(extension: Extension<T, *, *>) = copy(override = OverrideKind.Present(extension))

    fun newOrdering(before: Array<Extension<*, *, *>>, after: Array<Extension<*, *, *>>) {
        copy(ordering = OrderingKind.ByDsl {
            before(*before)
            after(*after)
        })
    }

    fun addCondition(c: (DokkaConfiguration) -> Boolean) = copy(conditions = conditions + c)

    fun name(name: String) = copy(name = name)
}

abstract class DokkaJavaPlugin: DokkaPlugin() {

    fun <T: DokkaPlugin> plugin(clazz: Class<T>): T? =
        context?.plugin(clazz.kotlin) ?: throwIllegalQuery()


    fun <T: Any> extend(func: (ExtensionBuilderStart) -> ExtensionBuilder<T>): Lazy<Extension<T, *, *>> =
        lazy { func(ExtensionBuilderStart()).build() }.also { unsafeInstall(it) }

}