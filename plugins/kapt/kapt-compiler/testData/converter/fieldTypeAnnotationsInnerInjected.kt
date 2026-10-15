// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|innerType|null|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|innerType|.|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|dollarInClassNames|null|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|dollarInClassNames|.|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|mixedTypePath|[.0;1;|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|mixedTypePath|[.0;1;*|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|interleavedArrayAndInnerTypes|[[.|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|interleavedArrayAndInnerTypes|[|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|nestedAndInnerType|null|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|nestedAndInnerType|.|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|null|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|.|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|..|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|...|Lorg/jetbrains/Anno;|visible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|...0;|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|....|Lorg/jetbrains/SourceAnno;|invisible
// INJECT_FIELD_TYPE_ANNOTATION: org/jetbrains/FieldTypeAnnotationsInnerInjected|genericInnerChain|....0;|Lorg/jetbrains/Anno;|visible

package org.jetbrains

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Outer {
    inner class Inner

    class Nested {
        inner class Inner
    }
}

class GenericOuter<T> {
    inner class GenericInner<U>
}

class DeepOuter {
    inner class Middle {
        inner class DeepInner
    }
}

class `Dollar$Outer` {
    inner class `Inner$Class`
}

class ChainOuter {
    class Nested {
        inner class Inner1 {
            inner class Inner2 {
                inner class InnerT<T> {
                    inner class InnerK<K>
                }
            }
        }
    }
}

class FieldTypeAnnotationsInnerInjected {
    @JvmField
    val innerType: /*@Anno*/ Outer./*@SourceAnno*/Inner = Outer().Inner()

    @JvmField
    val dollarInClassNames: /*@Anno*/ `Dollar$Outer`./*@SourceAnno*/`Inner$Class` = `Dollar$Outer`().`Inner$Class`()

    @JvmField
    val mixedTypePath:
            Array<GenericOuter<Unit>.GenericInner<MutableMap<String, /*@Anno*/ out /*@SourceAnno*/ String>>> = emptyArray()

    @JvmField
    val interleavedArrayAndInnerTypes:
            Array</*@SourceAnno*/ Array<DeepOuter./*@Anno*/Middle.DeepInner>> = emptyArray()

    @JvmField
    val nestedAndInnerType: Outer./*@Anno*/Nested./*@SourceAnno*/Inner = Outer.Nested().Inner()

    @JvmField
    val genericInnerChain: ChainOuter./*@SourceAnno*/Nested./*@Anno*/Inner1./*@SourceAnno*/Inner2.
            /*@Anno*/InnerT</*@SourceAnno*/String>./*@SourceAnno*/InnerK</*@Anno*/String>? = null
}
