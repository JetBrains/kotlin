@file:OptIn(kotlinx.parcelize.Experimental::class)
package test

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.PolymorphicSealed
import kotlinx.parcelize.ParcelTag
import android.os.Parcelable

// @PolymorphicSealed on non-sealed classes
@Parcelize
@PolymorphicSealed
class <!POLYMORPHIC_SEALED_MUST_BE_SEALED!>Regular<!>(val x: String) : Parcelable

@Parcelize
@PolymorphicSealed
data class <!POLYMORPHIC_SEALED_MUST_BE_SEALED!>DataClass<!>(val x: String) : Parcelable

// @PolymorphicSealed without @Parcelize
@PolymorphicSealed
sealed class <!POLYMORPHIC_SEALED_WITHOUT_PARCELIZE!>WithoutParcelize<!> : Parcelable

// Prohibited open subclass in hierarchy
@Parcelize
@PolymorphicSealed
sealed class SealedWithOpen : Parcelable {
    open class <!POLYMORPHIC_SEALED_CANNOT_HAVE_OPEN_SUBCLASSES!>OpenChild<!>(val x: Int) : SealedWithOpen()
}

// Prohibited non-sealed abstract subclass in hierarchy
@Parcelize
@PolymorphicSealed
sealed class SealedWithAbstract : Parcelable {
    <!PARCELABLE_SHOULD_BE_INSTANTIABLE!>abstract<!> class <!POLYMORPHIC_SEALED_CANNOT_HAVE_ABSTRACT_SUBCLASSES!>AbstractChild<!>(val x: Int) : SealedWithAbstract()
}

// Duplicate @ParcelTag values
@Parcelize
@PolymorphicSealed
sealed class DuplicateTags : Parcelable {
    @ParcelTag(1)
    data class A(val a: String) : DuplicateTags()

    <!DUPLICATE_PARCEL_TAG!>@ParcelTag(1)<!>
    data class B(val b: String) : DuplicateTags()
}

// Inconsistent @ParcelTag (All-or-Nothing rule)
@Parcelize
@PolymorphicSealed
sealed class InconsistentTags : Parcelable {
    @ParcelTag(1)
    data class A(val a: String) : InconsistentTags()

    data class <!INCONSISTENT_PARCEL_TAG!>B<!>(val b: String) : InconsistentTags()
}

// Inapplicable @ParcelTag on standalone or invalid targets
<!INAPPLICABLE_PARCEL_TAG!>@ParcelTag(1)<!>
class StandaloneClass(val x: String)

@Parcelize
@PolymorphicSealed
<!INAPPLICABLE_PARCEL_TAG!>@ParcelTag(1)<!>
sealed class SealedRootWithTag : Parcelable {
    @ParcelTag(2)
    data object Child : SealedRootWithTag()

    <!INAPPLICABLE_PARCEL_TAG!>@ParcelTag(3)<!>
    class NonSubType
}

// Prohibited nested sealed subclasses
@Parcelize
@PolymorphicSealed
sealed class DisallowNestedSealed : Parcelable {
    sealed class <!POLYMORPHIC_SEALED_CANNOT_HAVE_SEALED_SUBCLASSES!>NestedSealed<!> : DisallowNestedSealed()
}

// Prohibited subclasses declared outside the sealed class body
@Parcelize
@PolymorphicSealed
sealed class DisallowExternalSubclasses : Parcelable

class <!POLYMORPHIC_SEALED_SUBCLASS_MUST_BE_NESTED!>TopLevelSubclass<!>(val x: Int) : DisallowExternalSubclasses()

class OtherContainer {
    class <!POLYMORPHIC_SEALED_SUBCLASS_MUST_BE_NESTED!>OtherNestedSubclass<!>(val x: Int) : DisallowExternalSubclasses()
}

// Negative @ParcelTag values (valid, duplicate, inconsistent)
@Parcelize
@PolymorphicSealed
sealed class ValidNegativeTag : Parcelable {
    @ParcelTag(-1)
    data class A(val a: String) : ValidNegativeTag()

    @ParcelTag(1)
    data class B(val b: String) : ValidNegativeTag()
}

@Parcelize
@PolymorphicSealed
sealed class DuplicateNegativeTags : Parcelable {
    @ParcelTag(-10)
    data class A(val a: String) : DuplicateNegativeTags()

    <!DUPLICATE_PARCEL_TAG!>@ParcelTag(-10)<!>
    data class B(val b: String) : DuplicateNegativeTags()
}

@Parcelize
@PolymorphicSealed
sealed class InconsistentNegativeTags : Parcelable {
    @ParcelTag(-5)
    data class A(val a: String) : InconsistentNegativeTags()

    data class <!INCONSISTENT_PARCEL_TAG!>B<!>(val b: String) : InconsistentNegativeTags()
}

// Constant expressions as @ParcelTag values
private const val CONST_TAG_A = 100
private const val CONST_TAG_B = 100 + 1

@Parcelize
@PolymorphicSealed
sealed class ConstantExpressionTags : Parcelable {
    @ParcelTag(CONST_TAG_A)
    data object First : ConstantExpressionTags()

    @ParcelTag(CONST_TAG_B)
    data object Second : ConstantExpressionTags()

    @ParcelTag(100 + 2)
    data object Third : ConstantExpressionTags()
}

@Parcelize
@PolymorphicSealed
sealed class ConstantExpressionTagsDuplicated : Parcelable {
    @ParcelTag(CONST_TAG_A)
    data object First : ConstantExpressionTagsDuplicated()

    <!DUPLICATE_PARCEL_TAG!>@ParcelTag(CONST_TAG_B - 1)<!>
    data object Second : ConstantExpressionTagsDuplicated()

    <!DUPLICATE_PARCEL_TAG!>@ParcelTag(100)<!>
    data object Third : ConstantExpressionTagsDuplicated()
}

// Valid Sealed Class with Auto-assigned Tags
@Parcelize
@PolymorphicSealed
sealed class ValidAutoTags : Parcelable {
    data object First : ValidAutoTags()
    data class Second(val x: Int) : ValidAutoTags()
}

// Valid Sealed Class with Explicit Tags
@Parcelize
@PolymorphicSealed
sealed class ValidExplicitTags : Parcelable {
    @ParcelTag(0)
    data object First : ValidExplicitTags()

    @ParcelTag(10)
    data class Second(val x: Int) : ValidExplicitTags()
}

// Subclass implementing its enclosing @PolymorphicSealed class and another regular interface (Valid)
interface RegularOtherInterface

@Parcelize
@PolymorphicSealed
sealed class ValidWithOtherInterface : Parcelable {
    @ParcelTag(1)
    data class Child(val x: String) : ValidWithOtherInterface(), RegularOtherInterface
}

// Subclass inside @PolymorphicSealed interface implementing another @PolymorphicSealed interface with @ParcelTag
@Parcelize
@PolymorphicSealed
sealed interface TaggedInterfaceA : Parcelable {
    @ParcelTag(1)
    class <!MULTIPLE_POLYMORPHIC_SEALED_SUPERTYPES!>ChildInTaggedA<!>(val x: String) : TaggedInterfaceA, TaggedInterfaceB
}

@Parcelize
@PolymorphicSealed
sealed interface TaggedInterfaceB : Parcelable


