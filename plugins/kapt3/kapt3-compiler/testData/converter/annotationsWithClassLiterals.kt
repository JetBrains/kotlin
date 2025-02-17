import kotlin.reflect.KClass

@Repeatable
annotation class A(val clazz: KClass<*>)

annotation class As(val classes: Array<KClass<*>>)

@A(Boolean::class)
@A(Char::class)
@A(Byte::class)
@A(Short::class)
@A(Int::class)
@A(Float::class)
@A(Long::class)
@A(Double::class)

@A(BooleanArray::class)
@A(CharArray::class)
@A(ByteArray::class)
@A(ShortArray::class)
@A(IntArray::class)
@A(FloatArray::class)
@A(LongArray::class)
@A(DoubleArray::class)

@A(Any::class)
@A(Nothing::class)
@A(MutableList::class)
@A(Map::class)
@A(MutableMap.MutableEntry::class)
@A(Function0::class)
@A(Function1::class)
@A(Function22::class)

@A(Array<Int>::class)
@A(Array<IntArray>::class)
@A(Array<Array<IntArray>>::class)
@A(Array<Array<Array<LongArray>>>::class)
class SingleArgument


@As([Boolean::class])
@As([Char::class])
@As([Byte::class])
@As([Short::class])
@As([Int::class])
@As([Float::class])
@As([Long::class])
@As([Double::class])

@As([BooleanArray::class])
@As([CharArray::class])
@As([ByteArray::class])
@As([ShortArray::class])
@As([IntArray::class])
@As([FloatArray::class])
@As([LongArray::class])
@As([DoubleArray::class])

@As([Any::class])
@As([Nothing::class])
@As([MutableList::class])
@As([Map::class])
@As([MutableMap.MutableEntry::class])
@As([Function0::class])
@As([Function1::class])
@As([Function22::class])

@As([Array<Int>::class])
@As([Array<IntArray>::class])
@As([Array<Array<IntArray>>::class])
@As([Array<Array<Array<LongArray>>>::class])
class ArrayOfSingleArgument


@As([
    Boolean::class,
    Char::class,
    Byte::class,
    Short::class,
    Int::class,
    Float::class,
    Long::class,
    Double::class,

    BooleanArray::class,
    CharArray::class,
    ByteArray::class,
    ShortArray::class,
    IntArray::class,
    FloatArray::class,
    LongArray::class,
    DoubleArray::class,

    Any::class,
    Nothing::class,
    MutableList::class,
    Map::class,
    MutableMap.MutableEntry::class,
    Function0::class,
    Function1::class,
    Function22::class,

    Array<Int>::class,
    Array<IntArray>::class,
    Array<Array<IntArray>>::class,
    Array<Array<Array<LongArray>>>::class,
])
class ArrayOfAll


