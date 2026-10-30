// MODULE: main
// FILE: example/props.kt
package example

interface BaseProperties {
    val value: String
}

const val constant: Int = 42

external val externalProperty: String

inline val String.inlineSize: Int

abstract class ModifiableProperties {
    abstract val abstractValue: String

    lateinit var initialized: String

    open var openValue: String
}

interface Properties<T> {
    context(label: String)
    val contextual: T

    val immutable: String

    var mutable: Int

    val nested: Map<String, List<T?>>

    val nullable: String?
}

class PropertyOverrides : BaseProperties {
    final override val value: String
}
