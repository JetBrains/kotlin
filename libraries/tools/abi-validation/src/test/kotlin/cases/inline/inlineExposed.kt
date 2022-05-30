package cases.inline

@PublishedApi
internal fun exposedForInline() {}

@PublishedApi
internal class InternalClassExposed
    @PublishedApi
    internal constructor() {

    @PublishedApi
    internal fun funExposed() {}

    @PublishedApi
    internal var propertyExposed: String? = null

    @JvmField
    @PublishedApi
    internal var fieldExposed: String? = null
}
