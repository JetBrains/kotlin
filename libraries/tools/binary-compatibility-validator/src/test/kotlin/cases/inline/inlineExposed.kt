@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package cases.inline

@kotlin.internal.InlineExposed
internal fun exposedForInline() {}

@kotlin.internal.InlineExposed
internal class InternalClassExposed
    @kotlin.internal.InlineExposed
    internal constructor() {

    @kotlin.internal.InlineExposed
    internal fun funExposed() {}

    @kotlin.internal.InlineExposed
    internal var propertyExposed: String? = null

    @JvmField
    @kotlin.internal.InlineExposed
    internal var fieldExposed: String? = null

}
