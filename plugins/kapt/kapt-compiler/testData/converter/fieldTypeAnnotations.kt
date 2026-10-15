// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK

package org.jetbrains

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

enum class Mode { FIRST, SECOND }

annotation class Nested(val value: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class AnnoWithArguments(
    val text: String,
    val number: Int,
    val klass: KClass<*>,
    val mode: Mode,
    val nested: Nested,
    val numbers: IntArray,
)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnnoWithArguments(val text: String, val classes: Array<KClass<*>>)

@Repeatable
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class RepeatableAnno(val value: String)

@Repeatable
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class RepeatableSourceAnno(val value: String)

class Outer {
    class Nested
}

class GenericOuter<T> {
    class GenericNested<U>
}

class DeepOuter {
    class Middle {
        class DeepNested
    }
}

class `Dollar$Outer` {
    class `Nested$Class`
}

class FieldTypeAnnotations<T> {
    @JvmField
    val nullPath: @Anno @SourceAnno String = ""

    @JvmField
    val primitive: @Anno @SourceAnno Int = 0

    @JvmField
    val arrayElement: Array<@Anno @SourceAnno String> = emptyArray()

    @JvmField
    val outerArray: @Anno @SourceAnno Array<Array<String>> = emptyArray()

    @JvmField
    val allArrayDimensions: @Anno Array<@SourceAnno Array<@Anno @SourceAnno Array<String>>> = emptyArray()

    @JvmField
    val genericInnerArray: Array<@Anno @SourceAnno Array<List<String>>> = emptyArray()

    @JvmField
    val primitiveInnerArray: Array<@Anno @SourceAnno IntArray> = emptyArray()

    @JvmField
    val genericTypeArgument: List<@Anno @SourceAnno String> = emptyList()

    @JvmField
    var wildcardBound: MutableList<out @Anno @SourceAnno Number> = mutableListOf<Int>()

    @JvmField
    var wildcardLowerBound: MutableList<in @Anno @SourceAnno Number> = mutableListOf<Any>()

    @JvmField
    var typeVariable: @Anno @SourceAnno T? = null

    @JvmField
    val nestedType: @Anno @SourceAnno Outer.Nested = Outer.Nested()

    @JvmField
    val genericNestedType: @Anno @SourceAnno GenericOuter.GenericNested<String> = GenericOuter.GenericNested()

    @JvmField
    val classpathNestedType: @Anno @SourceAnno Thread.State = Thread.State.NEW

    @JvmField
    val dollarInClassNames: @Anno @SourceAnno `Dollar$Outer`.`Nested$Class` = `Dollar$Outer`.`Nested$Class`()

    @JvmField
    val nestedTypeInMultidimensionalArray: Array<@SourceAnno Array<@Anno DeepOuter.Middle.DeepNested>> = emptyArray()

    @JvmField
    val nestedTypeArgument: List<@Anno @SourceAnno Outer.Nested> = emptyList()

    @JvmField
    val dollarInTopLevelName: @Anno @SourceAnno `Dollar$Outer` = `Dollar$Outer`()

    @JvmField
    val mixedTypePath: @Anno Array<
            @SourceAnno GenericOuter.GenericNested<
                    @Anno MutableMap<String, out @Anno @SourceAnno Number>,
                    >,
            > = emptyArray()

    @JvmField
    val annotationArguments: @AnnoWithArguments("simple", 42, String::class, Mode.FIRST, Nested("nested"), [1, 2])
            @SourceAnnoWithArguments("source", [String::class, Int::class]) String = ""

    @JvmField
    val genericAnnotationArguments: List<@AnnoWithArguments("argument", -1, IntArray::class, Mode.SECOND, Nested("argument"), [])
            @SourceAnnoWithArguments("source argument", []) String> = emptyList()

    @JvmField
    val repeatableAnnotations: @RepeatableAnno("first") @RepeatableAnno("second")
            @RepeatableSourceAnno("first") @RepeatableSourceAnno("second") String = ""

    @JvmField
    val genericRepeatableAnnotations: List<@RepeatableAnno("argument first") @RepeatableAnno("argument second")
            @RepeatableSourceAnno("argument first") @RepeatableSourceAnno("argument second") String> = emptyList()
}
