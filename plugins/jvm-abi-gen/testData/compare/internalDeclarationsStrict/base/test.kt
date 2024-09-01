package test

internal fun funToBeRemoved() = Unit

internal fun internalTopLevelInlineFun() = Unit

internal class TopLevelClassToBeRemoved

fun emptyKtClassWorkaround() = Unit
class PublicOuterClass {

    @JvmField
    internal val fieldToBeRemoved = Unit

    internal interface InternalInterfaceToBeRemoved

    internal class InnerClass: InternalInterfaceToBeRemoved {
        fun everyDeclarationShouldBeRemoved() = Unit

        inline fun shouldBeRemoved() = Unit

        class EffectivelyInternalClassRemoved
    }

    internal interface TransitivelyPublicInterface

    @ExposedCopyVisibility
    data class Data internal constructor(
        val couldNotBeRemoved: String,
        internal val shouldBeRemoved: Any,
    ): TransitivelyPublicInterface

    internal inline fun shouldBeRemoved() = Unit
}
