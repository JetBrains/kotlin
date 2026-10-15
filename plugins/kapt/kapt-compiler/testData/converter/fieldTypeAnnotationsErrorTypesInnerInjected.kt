// CORRECT_ERROR_TYPES
// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// Kotlin source cannot place annotations on nested-class components; inject them at the exact
// paths a Java compiler would emit for the same declarations, including INNER_TYPE steps.
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|nestedClass|null|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|nestedClass|.|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|innerClassArgument|1;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|innerClassArgument|1;.|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|nestedInjectedInner|.|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|genericInnerChain|null|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|genericInnerChain|.|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|genericInnerChain|..|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|genericInnerChain|...|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|genericInnerChain|...0;|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|staticReset|null|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|staticReset|.|Lorg/jetbrains/SourceAnno;|invisible
// Kotlin/JVM does not emit these inner-class type-argument annotations yet, so inject their JVM paths.
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|corrected|0;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|corrected|.0;|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|twoInners|0;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|twoInners|.0;|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|twoInners|..0;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|importedTwoInners|0;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|importedTwoInners|.0;|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsErrorTypesInnerInjected|importedTwoInners|..0;|Lorg/jetbrains/Anno;|visible

@file:Suppress("UNRESOLVED_REFERENCE")

package org.jetbrains

import org.jetbrains.GenericOuter.StaticNested

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Outer {
    inner class Inner<T>
}

class GenericOuter<T> {
    inner class Inner<U>

    class StaticNested<U> {
        inner class Inner1<V> {
            inner class Inner2<W>
        }
    }
}

class ChainOuter {
    class Nested {
        inner class Inner1 {
            inner class Inner2 {
                inner class InnerT<T, U>

                class StaticNested {
                    inner class InnerAfter<T>
                }
            }
        }
    }
}

class FieldTypeAnnotationsErrorTypesInnerInjected {
    // The outer-level annotation belongs on the outermost class, the INNER_TYPE-step annotation on
    // the nested component; error type correction must keep both on their levels.
    @JvmField
    val nestedClass: /*@SourceAnno*/ Outer./*@Anno*/Inner<Missing>? = null

    // The same rule inside a type argument of an otherwise erroneous type.
    @JvmField
    val innerClassArgument: Map<Missing, /*@Anno*/ Outer./*@SourceAnno*/Inner<Missing>>? = null

    @JvmField
    val nestedInjectedInner: Outer./*@SourceAnno*/Inner<Missing>? = null

    @JvmField
    val genericInnerChain: ChainOuter./*@SourceAnno*/Nested./*@Anno*/Inner1./*@SourceAnno*/Inner2.
            /*@Anno*/InnerT</*@SourceAnno*/String, Missing>? = null

    @JvmField
    val staticReset: ChainOuter.Nested.Inner1.Inner2./*@Anno*/StaticNested./*@SourceAnno*/InnerAfter<Missing>? = null

    @JvmField
    val corrected: GenericOuter</*@Anno*/String>.Inner</*@SourceAnno*/List<Missing>>? = null

    @JvmField
    val twoInners: GenericOuter.StaticNested</*@Anno*/String>.Inner1</*@SourceAnno*/Int>.
            Inner2</*@Anno*/List<Missing>>? = null

    @JvmField
    val importedTwoInners: StaticNested</*@Anno*/String>.Inner1</*@SourceAnno*/Int>.
            Inner2</*@Anno*/List<Missing>>? = null
}
