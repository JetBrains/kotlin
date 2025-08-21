/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.*

public class ClassReference(override val jClass: Class<*>) : KClass<Any>, ClassBasedDeclarationContainer {
    override val simpleName: String?
        get() = getClassSimpleName(jClass)

    override val qualifiedName: String?
        get() = getClassQualifiedName(jClass)

    override val members: Collection<KCallable<*>>
        get() = error()

    override val constructors: Collection<KFunction<Any>>
        get() = error()

    override val nestedClasses: Collection<KClass<*>>
        get() = error()

    override val annotations: List<Annotation>
        get() = error()

    override val objectInstance: Any?
        get() = error()

    @SinceKotlin("1.1")
    override fun isInstance(value: Any?): Boolean =
        isInstance(value, jClass)

    @SinceKotlin("1.1")
    override val typeParameters: List<KTypeParameter>
        get() = error()

    @SinceKotlin("1.1")
    override val supertypes: List<KType>
        get() = error()

    @SinceKotlin("1.3")
    override val sealedSubclasses: List<KClass<out Any>>
        get() = error()

    @SinceKotlin("1.1")
    override val visibility: KVisibility?
        get() = error()

    @SinceKotlin("1.1")
    override val isFinal: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isOpen: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isAbstract: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isSealed: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isData: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isInner: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isCompanion: Boolean
        get() = error()

    @SinceKotlin("1.4")
    override val isFun: Boolean
        get() = error()

    @SinceKotlin("1.5")
    override val isValue: Boolean
        get() = error()

    private fun error(): Nothing = throw KotlinReflectionNotSupportedError()

    override fun equals(other: Any?): Boolean =
        other is ClassReference && javaObjectType == other.javaObjectType

    override fun hashCode(): Int =
        javaObjectType.hashCode()

    override fun toString(): String =
        jClass.toString() + Reflection.REFLECTION_NOT_AVAILABLE

    public companion object {
        private val FUNCTION_CLASSES =
            listOf(
                Function0::class.java, Function1::class.java, Function2::class.java, Function3::class.java, Function4::class.java,
                Function5::class.java, Function6::class.java, Function7::class.java, Function8::class.java, Function9::class.java,
                Function10::class.java, Function11::class.java, Function12::class.java, Function13::class.java, Function14::class.java,
                Function15::class.java, Function16::class.java, Function17::class.java, Function18::class.java, Function19::class.java,
                Function20::class.java, Function21::class.java, Function22::class.java
            ).mapIndexed { i, clazz -> clazz to i }.toMap()

        // See JavaToKotlinClassMap.
        private fun classFqNameOf(type: String) = when (type) {
            "boolean" -> "kotlin.Boolean"
            "byte" -> "kotlin.Byte"
            "char" -> "kotlin.Char"
            "double" -> "kotlin.Double"
            "float" -> "kotlin.Float"
            "int" -> "kotlin.Int"
            "long" -> "kotlin.Long"
            "short" -> "kotlin.Short"
            "java.lang.annotation.Annotation" -> "kotlin.Annotation"
            "java.lang.Boolean" -> "kotlin.Boolean"
            "java.lang.Byte" -> "kotlin.Byte"
            "java.lang.Character" -> "kotlin.Char"
            "java.lang.CharSequence" -> "kotlin.CharSequence"
            "java.lang.Cloneable" -> "kotlin.Cloneable"
            "java.lang.Comparable" -> "kotlin.Comparable"
            "java.lang.Double" -> "kotlin.Double"
            "java.lang.Enum" -> "kotlin.Enum"
            "java.lang.Float" -> "kotlin.Float"
            "java.lang.Integer" -> "kotlin.Int"
            "java.lang.Iterable" -> "kotlin.collections.Iterable"
            "java.lang.Long" -> "kotlin.Long"
            "java.lang.Number" -> "kotlin.Number"
            "java.lang.Object" -> "kotlin.Any"
            "java.lang.Short" -> "kotlin.Short"
            "java.lang.String" -> "kotlin.String"
            "java.lang.Throwable" -> "kotlin.Throwable"
            "java.util.Collection" -> "kotlin.collections.Collection"
            "java.util.Iterator" -> "kotlin.collections.Iterator"
            "java.util.ListIterator" -> "kotlin.collections.ListIterator"
            "java.util.List" -> "kotlin.collections.List"
            "java.util.Map\$Entry" -> "kotlin.collections.Map.Entry"
            "java.util.Map" -> "kotlin.collections.Map"
            "java.util.Set" -> "kotlin.collections.Set"
            "kotlin.jvm.functions.Function0" -> "kotlin.Function0"
            "kotlin.jvm.functions.Function1" -> "kotlin.Function1"
            "kotlin.jvm.functions.Function2" -> "kotlin.Function2"
            "kotlin.jvm.functions.Function3" -> "kotlin.Function3"
            "kotlin.jvm.functions.Function4" -> "kotlin.Function4"
            "kotlin.jvm.functions.Function5" -> "kotlin.Function5"
            "kotlin.jvm.functions.Function6" -> "kotlin.Function6"
            "kotlin.jvm.functions.Function7" -> "kotlin.Function7"
            "kotlin.jvm.functions.Function8" -> "kotlin.Function8"
            "kotlin.jvm.functions.Function9" -> "kotlin.Function9"
            "kotlin.jvm.functions.Function10" -> "kotlin.Function10"
            "kotlin.jvm.functions.Function11" -> "kotlin.Function11"
            "kotlin.jvm.functions.Function12" -> "kotlin.Function12"
            "kotlin.jvm.functions.Function13" -> "kotlin.Function13"
            "kotlin.jvm.functions.Function14" -> "kotlin.Function14"
            "kotlin.jvm.functions.Function15" -> "kotlin.Function15"
            "kotlin.jvm.functions.Function16" -> "kotlin.Function16"
            "kotlin.jvm.functions.Function17" -> "kotlin.Function17"
            "kotlin.jvm.functions.Function18" -> "kotlin.Function18"
            "kotlin.jvm.functions.Function19" -> "kotlin.Function19"
            "kotlin.jvm.functions.Function20" -> "kotlin.Function20"
            "kotlin.jvm.functions.Function21" -> "kotlin.Function21"
            "kotlin.jvm.functions.Function22" -> "kotlin.Function22"
            "kotlin.jvm.internal.BooleanCompanionObject" -> "kotlin.Boolean.Companion"
            "kotlin.jvm.internal.ByteCompanionObject" -> "kotlin.Byte.Companion"
            "kotlin.jvm.internal.CharCompanionObject" -> "kotlin.Char.Companion"
            "kotlin.jvm.internal.DoubleCompanionObject" -> "kotlin.Double.Companion"
            "kotlin.jvm.internal.EnumCompanionObject" -> "kotlin.Enum.Companion"
            "kotlin.jvm.internal.FloatCompanionObject" -> "kotlin.Float.Companion"
            "kotlin.jvm.internal.IntCompanionObject" -> "kotlin.Int.Companion"
            "kotlin.jvm.internal.LongCompanionObject" -> "kotlin.Long.Companion"
            "kotlin.jvm.internal.ShortCompanionObject" -> "kotlin.Short.Companion"
            "kotlin.jvm.internal.StringCompanionObject" -> "kotlin.String.Companion"
            else -> null
        }

        private fun simpleNameOf(type: String) = when (type) {
            "boolean" -> "Boolean"
            "byte" -> "Byte"
            "char" -> "Char"
            "double" -> "Double"
            "float" -> "Float"
            "int" -> "Int"
            "long" -> "Long"
            "short" -> "Short"
            "java.lang.annotation.Annotation" -> "Annotation"
            "java.lang.Boolean" -> "Boolean"
            "java.lang.Byte" -> "Byte"
            "java.lang.Character" -> "Char"
            "java.lang.CharSequence" -> "CharSequence"
            "java.lang.Cloneable" -> "Cloneable"
            "java.lang.Comparable" -> "Comparable"
            "java.lang.Double" -> "Double"
            "java.lang.Enum" -> "Enum"
            "java.lang.Float" -> "Float"
            "java.lang.Integer" -> "Int"
            "java.lang.Iterable" -> "Iterable"
            "java.lang.Long" -> "Long"
            "java.lang.Number" -> "Number"
            "java.lang.Object" -> "Any"
            "java.lang.Short" -> "Short"
            "java.lang.String" -> "String"
            "java.lang.Throwable" -> "Throwable"
            "java.util.Collection" -> "Collection"
            "java.util.Iterator" -> "Iterator"
            "java.util.ListIterator" -> "ListIterator"
            "java.util.List" -> "List"
            "java.util.Map\$Entry" -> "Entry"
            "java.util.Map" -> "Map"
            "java.util.Set" -> "Set"
            "kotlin.jvm.functions.Function0" -> "Function0"
            "kotlin.jvm.functions.Function1" -> "Function1"
            "kotlin.jvm.functions.Function2" -> "Function2"
            "kotlin.jvm.functions.Function3" -> "Function3"
            "kotlin.jvm.functions.Function4" -> "Function4"
            "kotlin.jvm.functions.Function5" -> "Function5"
            "kotlin.jvm.functions.Function6" -> "Function6"
            "kotlin.jvm.functions.Function7" -> "Function7"
            "kotlin.jvm.functions.Function8" -> "Function8"
            "kotlin.jvm.functions.Function9" -> "Function9"
            "kotlin.jvm.functions.Function10" -> "Function10"
            "kotlin.jvm.functions.Function11" -> "Function11"
            "kotlin.jvm.functions.Function12" -> "Function12"
            "kotlin.jvm.functions.Function13" -> "Function13"
            "kotlin.jvm.functions.Function14" -> "Function14"
            "kotlin.jvm.functions.Function15" -> "Function15"
            "kotlin.jvm.functions.Function16" -> "Function16"
            "kotlin.jvm.functions.Function17" -> "Function17"
            "kotlin.jvm.functions.Function18" -> "Function18"
            "kotlin.jvm.functions.Function19" -> "Function19"
            "kotlin.jvm.functions.Function20" -> "Function20"
            "kotlin.jvm.functions.Function21" -> "Function21"
            "kotlin.jvm.functions.Function22" -> "Function22"
            "kotlin.jvm.internal.BooleanCompanionObject" -> "Companion"
            "kotlin.jvm.internal.ByteCompanionObject" -> "Companion"
            "kotlin.jvm.internal.CharCompanionObject" -> "Companion"
            "kotlin.jvm.internal.DoubleCompanionObject" -> "Companion"
            "kotlin.jvm.internal.EnumCompanionObject" -> "Companion"
            "kotlin.jvm.internal.FloatCompanionObject" -> "Companion"
            "kotlin.jvm.internal.IntCompanionObject" -> "Companion"
            "kotlin.jvm.internal.LongCompanionObject" -> "Companion"
            "kotlin.jvm.internal.ShortCompanionObject" -> "Companion"
            "kotlin.jvm.internal.StringCompanionObject" -> "Companion"
            else -> null
        }

        public fun getClassSimpleName(jClass: Class<*>): String? = when {
            jClass.isAnonymousClass -> null
            jClass.isLocalClass -> {
                val name = jClass.simpleName
                jClass.enclosingMethod?.let { method -> name.substringAfter(method.name + "$") }
                    ?: jClass.enclosingConstructor?.let { constructor -> name.substringAfter(constructor.name + "$") }
                    ?: name.substringAfter('$')
            }
            jClass.isArray -> {
                val componentType = jClass.componentType
                when {
                    componentType.isPrimitive -> simpleNameOf(componentType.name)?.plus("Array")
                    else -> null
                } ?: "Array"
            }
            else -> simpleNameOf(jClass.name) ?: jClass.simpleName
        }

        public fun getClassQualifiedName(jClass: Class<*>): String? = when {
            jClass.isAnonymousClass -> null
            jClass.isLocalClass -> null
            jClass.isArray -> {
                val componentType = jClass.componentType
                when {
                    componentType.isPrimitive -> classFqNameOf(componentType.name)?.plus("Array")
                    else -> null
                } ?: "kotlin.Array"
            }
            else -> classFqNameOf(jClass.name) ?: jClass.canonicalName
        }

        public fun isInstance(value: Any?, jClass: Class<*>): Boolean {
            FUNCTION_CLASSES[jClass]?.let { arity ->
                return TypeIntrinsics.isFunctionOfArity(value, arity)
            }
            val objectType = if (jClass.isPrimitive) jClass.kotlin.javaObjectType else jClass
            return objectType.isInstance(value)
        }
    }
}
