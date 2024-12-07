// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
@file:Suppress("DEPRECATION_ERROR")

@Deprecated("Deprecated")
fun deprecatedImplicitlyF() = Unit
@Deprecated("Deprecated")
val deprecationInheritedImplicitlyV: Unit get() = Unit
@Deprecated("Deprecated")
typealias deprecatedImplicitlyA = Unit

@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
fun deprecatedF() = Unit
@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
val deprecationInheritedV: Unit get() = Unit
@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
typealias deprecatedA = Unit

@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
fun renamedF() = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
val renamedV: Unit get() = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
typealias renamedA = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
class renamedT

open class normalT {
    open class normalT
    open fun normalF() = Unit
    open val normalV: Unit get() = Unit
    open var normalP: Int
        get() = 42
        set(new) {}

    open fun deprecatedInFutureF() = Unit
    open val deprecatedInFutureV: Unit get() = Unit
    open var deprecatedInFutureP: Int
        get() = 42
        set(new) {}

    open fun obsoletedInFutureF() = Unit
    open val obsoletedInFutureV: Unit get() = Unit
    open var obsoletedInFutureP: Int
        get() = 42
        set(new) {}

    open fun removedInFutureF() = Unit
    open val removedInFutureV: Unit get() = Unit
    open var removedInFutureP: Int
        get() = 42
        set(new) {}

    @Deprecated("Deprecated")
    open class deprecatedT
    @Deprecated("Deprecated")
    constructor(deprecated: Int) {}
    @Deprecated("Deprecated")
    open fun deprecatedF() = Unit
    @Deprecated("Deprecated")
    open val deprecatedV: Unit get() = Unit
    open var deprecatedP: Int
        @Deprecated("Deprecated") get() = 42
        @Deprecated("Deprecated") set(new) {}

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open class obsoletedT
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    constructor(obsoleted: Float) {}
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open fun obsoletedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open val obsoletedV: Unit get() = Unit
    open var obsoletedP: Int
        @Deprecated("Obsoleted", level = DeprecationLevel.ERROR) get() = 42
        @Deprecated("Obsoleted", level = DeprecationLevel.ERROR) set(new) {}

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open class removedT
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    constructor(removed: Boolean) {}
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open fun removedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open val removedV: Unit get() = Unit
    open var removedP: Int
        @Deprecated("Removed", level = DeprecationLevel.HIDDEN) get() = 42
        @Deprecated("Removed", level = DeprecationLevel.HIDDEN) set(new) {}
}

class normalChildT : normalT() {
    override fun normalF() = Unit
    override val normalV: Unit get() = Unit

    @Deprecated("Deprecated", level = DeprecationLevel.WARNING)
    override fun deprecatedInFutureF() = Unit
    @Deprecated("Deprecated", level = DeprecationLevel.WARNING)
    override val deprecatedInFutureV: Unit get() = Unit
    @Deprecated("Deprecated", level = DeprecationLevel.WARNING)
    override var deprecatedInFutureP: Int
        @Deprecated("Deprecated", level = DeprecationLevel.WARNING) get() = 43
        @Deprecated("Deprecated", level = DeprecationLevel.WARNING) set(new) {}

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    override fun obsoletedInFutureF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    override val obsoletedInFutureV: Unit get() = Unit
    @Deprecated("Deprecated", level = DeprecationLevel.ERROR)
    override var obsoletedInFutureP: Int
        @Deprecated("Deprecated", level = DeprecationLevel.ERROR) get() = 43
        @Deprecated("Deprecated", level = DeprecationLevel.ERROR) set(new) {}

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    override fun removedInFutureF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    override val removedInFutureV: Unit get() = Unit
    @Deprecated("Deprecated", level = DeprecationLevel.HIDDEN)
    override var removedInFutureP: Int
        @Deprecated("Deprecated", level = DeprecationLevel.HIDDEN) get() = 43
        @Deprecated("Deprecated", level = DeprecationLevel.HIDDEN) set(new) {}

    override fun deprecatedF() = Unit
    override val deprecatedV: Unit get() = Unit
    override var deprecatedP: Int
        @Deprecated("Deprecated") get() = 42
        @Deprecated("Deprecated") set(new) {}

    override fun obsoletedF() = Unit
    override val obsoletedV: Unit get() = Unit
    override var obsoletedP: Int
        @Deprecated("Obsoleted", level = DeprecationLevel.ERROR) get() = 42
        @Deprecated("Obsoleted", level = DeprecationLevel.ERROR) set(new) {}


    override fun removedF() = Unit
    override val removedV: Unit get() = Unit
    override var removedP: Int
        @Deprecated("Removed", level = DeprecationLevel.HIDDEN) get() = 42
        @Deprecated("Removed", level = DeprecationLevel.HIDDEN) set(new) {}
}

@Deprecated("Deprecated")
open class deprecatedT {
    @Deprecated("Deprecated", level = DeprecationLevel.WARNING)
    constructor() {}

    open fun deprecationInheritedF() = Unit
    open val deprecationInheritedV: Unit get() = Unit
    open class deprecationInheritedT

    @Deprecated("Deprecated")
    open class deprecationRestatedT
    @Deprecated("Deprecated")
    open fun deprecationRestatedF() = Unit
    @Deprecated("Deprecated")
    open val deprecationRestatedV: Unit get() = Unit

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open class deprecationReinforcedT
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open fun deprecationReinforcedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open val deprecationReinforcedV: Unit get() = Unit

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open class deprecationFurtherReinforcedT
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open fun deprecationFurtherReinforcedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open val deprecationFurtherReinforcedV: Unit get() = Unit
}

class deprecatedChildT : deprecatedT() {
    override fun deprecationRestatedF() = Unit
    override val deprecationRestatedV: Unit get() = Unit
    override fun deprecationReinforcedF() = Unit
    override val deprecationReinforcedV: Unit get() = Unit
    override fun deprecationFurtherReinforcedF() = Unit
    override val deprecationFurtherReinforcedV: Unit get() = Unit
}

@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
fun obsoletedF() = Unit
@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
val obsoletedV: Unit get() = Unit
@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
typealias obsoletedA = Unit

@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
open class obsoletedT {
    @Deprecated("Deprecated", level = DeprecationLevel.ERROR)
    constructor() {}

    open fun deprecationInheritedF() = Unit
    open val deprecationInheritedV: Unit get() = Unit
    open class deprecationInheritedT

    @Deprecated("Deprecated")
    open class deprecationRelaxedT
    @Deprecated("Deprecated")
    open fun deprecationRelaxedF() = Unit
    @Deprecated("Deprecated")
    open val deprecationRelaxedV: Unit get() = Unit

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open class deprecationRestatedT
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open fun deprecationRestatedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open val deprecationRestatedV: Unit get() = Unit

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open class deprecationReinforcedT
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open fun deprecationReinforcedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open val deprecationReinforcedV: Unit get() = Unit
}

open class obsoletedChildT : obsoletedT() {
    override fun deprecationRelaxedF() = Unit
    override val deprecationRelaxedV: Unit get() = Unit
    override fun deprecationRestatedF() = Unit
    override val deprecationRestatedV: Unit get() = Unit
    override fun deprecationReinforcedF() = Unit
    override val deprecationReinforcedV: Unit get() = Unit
}

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
fun hiddenF() = Unit
@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
val hiddenV: Unit get() = Unit
@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
typealias hiddenA = Unit

@Deprecated("Removed", level = DeprecationLevel.HIDDEN)
open class hiddenT() {
    @Deprecated("Deprecated", level = DeprecationLevel.HIDDEN)
    constructor(int: Int) : this() {}

    open fun deprecationInheritedF() = Unit
    open val deprecationInheritedV: Unit get() = Unit
    open class deprecationInheritedT

    @Deprecated("Deprecated")
    open class deprecationFurtherRelaxedT
    @Deprecated("Deprecated")
    open fun deprecationFurtherRelaxedF() = Unit
    @Deprecated("Deprecated")
    open val deprecationFurtherRelaxedV: Unit get() = Unit

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open class deprecationRelaxedT
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open fun deprecationRelaxedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    open val deprecationRelaxedV: Unit get() = Unit

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open class deprecationRestatedT
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open fun deprecationRestatedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    open val deprecationRestatedV: Unit get() = Unit
}

open class hiddenСhildT : hiddenT() {
    override fun deprecationFurtherRelaxedF() = Unit
    override val deprecationFurtherRelaxedV: Unit get() = Unit
    override fun deprecationRelaxedF() = Unit
    override val deprecationRelaxedV: Unit get() = Unit
    override fun deprecationRestatedF() = Unit
    override val deprecationRestatedV: Unit get() = Unit
}

// FILE: annotations_replacewith.kt

const val MESSAGE = "message"

@Deprecated(message = MESSAGE)
fun constMessage(): Nothing = TODO("never")

@Deprecated(message = "->$MESSAGE<-")
fun formattedMessage(): Nothing = TODO("never")

@Deprecated(message = """
    line1
    line2
""")
fun multilineMessage(): Nothing = TODO("never")

@Deprecated(message = """
    line1
    $MESSAGE
    line2
""")
fun multilineFormattedMessage(): Nothing = TODO("never")

@Deprecated(message = "", replaceWith = ReplaceWith("unrenamed"))
fun unrenamed(): Nothing = TODO("never")

@Deprecated(message = "", replaceWith = ReplaceWith("something"))
fun renamed(x: Int, y: Float): Nothing = TODO("never")

@Deprecated(message = "", replaceWith = ReplaceWith("something(y, x)"))
fun renamedWithArguments(x: Int, y: Float): Nothing = TODO("never")

@Deprecated(message = "", replaceWith = ReplaceWith("something.else"))
fun renamedQualified(x: Int, y: Float): Nothing = TODO("never")

@Deprecated(message = "", replaceWith = ReplaceWith("something.else(x, y)"))
fun renamedQualifiedWithArguments(x: Int, y: Float): Nothing = TODO("never")
