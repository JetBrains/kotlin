@file:Suppress("UNUSED_PARAMETER")
package cases.default

class ClassConstructors
internal constructor(name: String, flags: Int = 0) {

    internal constructor(name: StringBuilder, flags: Int = 0) : this(name.toString(), flags)

}

