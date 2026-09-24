// LANGUAGE: -FullValueClasses
// WITH_STDLIB
// ISSUE: KT-89640

import lombok.NoArgsConstructor

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class OnValueClassWithoutJvmInline(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
@JvmInline
value class OnInlineClass(val value: Int)

<!ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS!>@NoArgsConstructor(force = true)<!>
<!INLINE_CLASS_DEPRECATED!>inline<!> class OnDeprecatedInlineClass(val value: Int)

fun test() {
    <!NO_VALUE_FOR_PARAMETER!>OnValueClassWithoutJvmInline<!>()
    <!NO_VALUE_FOR_PARAMETER!>OnInlineClass<!>()
    <!NO_VALUE_FOR_PARAMETER!>OnDeprecatedInlineClass<!>()
}
