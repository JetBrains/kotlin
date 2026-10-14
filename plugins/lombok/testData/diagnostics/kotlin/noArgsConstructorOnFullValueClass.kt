// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-89640

import lombok.NoArgsConstructor

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
value class OnFullValueClass(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(staticName = "create", force = true)<!>
value class OnFullValueClassWithStaticName(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
value class OnMultiFieldValueClass(val x: Int, val y: String)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
@JvmInline
value class OnInlineClass(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class OnDeprecatedInlineClass(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor<!>
abstract value class OnAbstractValueClass(x: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor<!>
sealed value class OnSealedValueClass(x: Int)

value class ExtendsAbstractValueClass(val x: Int) : <!NO_VALUE_FOR_PARAMETER!>OnAbstractValueClass<!>()

value class ExtendsSealedValueClass(val x: Int) : <!NO_VALUE_FOR_PARAMETER!>OnSealedValueClass<!>()

abstract value class AbstractValueClassWithNoArgs

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
value class OnValueClassExtendingValueClass(val x: Int) : AbstractValueClassWithNoArgs()

@NoArgsConstructor
class IdentityClassExtendingValueClass(var x: Int) : AbstractValueClassWithNoArgs()

fun test() {
    <!NO_VALUE_FOR_PARAMETER!>OnFullValueClass<!>()
    OnFullValueClassWithStaticName.<!UNRESOLVED_REFERENCE!>create<!>()
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>OnMultiFieldValueClass<!>()
    <!NO_VALUE_FOR_PARAMETER!>OnInlineClass<!>()
    <!NO_VALUE_FOR_PARAMETER!>OnDeprecatedInlineClass<!>()
    <!NO_VALUE_FOR_PARAMETER!>OnValueClassExtendingValueClass<!>()
    IdentityClassExtendingValueClass()
}
