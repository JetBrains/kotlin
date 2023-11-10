/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

public class ExtensionBuilderStart internal constructor(){
    public fun <T: Any>  extensionPoint(ext: ExtensionPoint<T>): ProvidedExtension<T> = ProvidedExtension(ext)
}

public class ProvidedExtension<T: Any> internal constructor(
    public val ext: ExtensionPoint<T>
) {
    public fun fromInstance(inst: T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromInstance(
            inst
        )
    )
    public fun fromRecipe(recipe: (DokkaContext) -> T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromRecipe(recipe)
    )

    private val defaultName = "${ext.pointName}/in/${javaClass.simpleName}"

    private fun createBuilder(action: LazyEvaluated<T>) =
        ExtensionBuilder(defaultName, ext, action,
            emptyList(), emptyList(),
            OverrideKind.None, emptyList())
}

public data class ExtensionBuilder<T: Any> internal constructor(
    private val name: String,
    private val ext: ExtensionPoint<T>,
    private val action: LazyEvaluated<T>,
    private val before: List<Extension<*, *, *>>,
    private val after: List<Extension<*, *, *>>,
    private val override: OverrideKind = OverrideKind.None,
    private val conditions: List<(DokkaConfiguration) -> Boolean>
){
    public fun build(): Extension<T, *, *>  = Extension(
        ext,
        javaClass.name,
        name,
        action,
        OrderingKind.ByDsl {
            before(*before.toTypedArray())
            after(*after.toTypedArray())
        },
        override,
        conditions
    )

    public fun overrideExtension(extension: Extension<T, *, *>): ExtensionBuilder<T> = copy(override = OverrideKind.Present(listOf(extension)))

    public fun newOrdering(before: Array<out Extension<*, *, *>>, after: Array<out Extension<*, *, *>>): ExtensionBuilder<T> =
        copy(before = this.before + before, after = this.after + after)

    public fun before(vararg exts: Extension<*, *, *>): ExtensionBuilder<T> = copy(before = this.before + exts)

    public fun after(vararg exts: Extension<*, *, *>): ExtensionBuilder<T> = copy(after = this.after + exts)

    public fun addCondition(c: (DokkaConfiguration) -> Boolean): ExtensionBuilder<T> = copy(conditions = conditions + c)

    public fun name(name: String): ExtensionBuilder<T> = copy(name = name)
}

public abstract class DokkaJavaPlugin: DokkaPlugin() {

    public fun <T: DokkaPlugin> plugin(clazz: Class<T>): T =
        context?.plugin(clazz.kotlin) ?: throwIllegalQuery()


    public fun <T: Any> extend(func: (ExtensionBuilderStart) -> ExtensionBuilder<T>): Lazy<Extension<T, *, *>> =
        lazy { func(ExtensionBuilderStart()).build() }.also { unsafeInstall(it) }

}
