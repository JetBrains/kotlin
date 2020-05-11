
public abstract class AbstractMutableList</*0*/ E> : kotlin.collections.AbstractMutableCollection<E>, kotlin.collections.MutableList<E> {
    /*primary*/ protected constructor AbstractMutableList</*0*/ E>()
    protected final var modCount: kotlin.Int
        protected final fun <get-modCount>(): kotlin.Int
        protected final fun <set-modCount>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open override /*2*/ fun add(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun add(/*0*/ index: kotlin.Int, /*1*/ element: E): kotlin.Unit
    public open override /*1*/ fun addAll(/*0*/ index: kotlin.Int, /*1*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*2*/ fun clear(): kotlin.Unit
    public open override /*2*/ fun contains(/*0*/ element: E): kotlin.Boolean
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun indexOf(/*0*/ element: E): kotlin.Int
    public open override /*2*/ fun iterator(): kotlin.collections.MutableIterator<E>
    public open override /*1*/ fun lastIndexOf(/*0*/ element: E): kotlin.Int
    public open override /*1*/ fun listIterator(): kotlin.collections.MutableListIterator<E>
    public open override /*1*/ fun listIterator(/*0*/ index: kotlin.Int): kotlin.collections.MutableListIterator<E>
    public open override /*2*/ fun removeAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun removeAt(/*0*/ index: kotlin.Int): E
    protected open fun removeRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.Unit
    public open override /*2*/ fun retainAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun set(/*0*/ index: kotlin.Int, /*1*/ element: E): E
    public open override /*1*/ fun subList(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.collections.MutableList<E>
}

public abstract class AbstractMutableMap</*0*/ K, /*1*/ V> : kotlin.collections.AbstractMap<K, V>, kotlin.collections.MutableMap<K, V> {
    /*primary*/ protected constructor AbstractMutableMap</*0*/ K, /*1*/ V>()
    public open override /*2*/ val keys: kotlin.collections.MutableSet<K>
        public open override /*2*/ fun <get-keys>(): kotlin.collections.MutableSet<K>
    public open override /*2*/ val values: kotlin.collections.MutableCollection<V>
        public open override /*2*/ fun <get-values>(): kotlin.collections.MutableCollection<V>
    public open override /*1*/ fun clear(): kotlin.Unit
    public abstract override /*1*/ fun put(/*0*/ key: K, /*1*/ value: V): V?
    public open override /*1*/ fun putAll(/*0*/ from: kotlin.collections.Map<out K, V>): kotlin.Unit
    public open override /*1*/ fun remove(/*0*/ key: K): V?
}

public abstract class AbstractMutableSet</*0*/ E> : kotlin.collections.AbstractMutableCollection<E>, kotlin.collections.MutableSet<E> {
    /*primary*/ protected constructor AbstractMutableSet</*0*/ E>()
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
}

@kotlin.SinceKotlin(version = "1.1") public abstract class AbstractSet</*0*/ out E> : kotlin.collections.AbstractCollection<E>, kotlin.collections.Set<E> {
    /*primary*/ protected constructor AbstractSet</*0*/ out E>()
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public final class ArrayDeque</*0*/ E> : kotlin.collections.AbstractMutableList<E> {
    public constructor ArrayDeque</*0*/ E>()
    public constructor ArrayDeque</*0*/ E>(/*0*/ initialCapacity: kotlin.Int)
    public constructor ArrayDeque</*0*/ E>(/*0*/ elements: kotlin.collections.Collection<E>)
    public open override /*1*/ var size: kotlin.Int
        public open override /*1*/ fun <get-size>(): kotlin.Int
        private open fun <set-size>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open override /*1*/ fun add(/*0*/ element: E): kotlin.Boolean
    public open override /*1*/ fun add(/*0*/ index: kotlin.Int, /*1*/ element: E): kotlin.Unit
    public open override /*1*/ fun addAll(/*0*/ index: kotlin.Int, /*1*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*1*/ fun addAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public final fun addFirst(/*0*/ element: E): kotlin.Unit
    public final fun addLast(/*0*/ element: E): kotlin.Unit
    public open override /*1*/ fun clear(): kotlin.Unit
    public open override /*1*/ fun contains(/*0*/ element: E): kotlin.Boolean
    public final fun first(): E
    public final fun firstOrNull(): E?
    public open override /*1*/ fun get(/*0*/ index: kotlin.Int): E
    public open override /*1*/ fun indexOf(/*0*/ element: E): kotlin.Int
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public final fun last(): E
    public open override /*1*/ fun lastIndexOf(/*0*/ element: E): kotlin.Int
    public final fun lastOrNull(): E?
    public open override /*1*/ fun remove(/*0*/ element: E): kotlin.Boolean
    public open override /*1*/ fun removeAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*1*/ fun removeAt(/*0*/ index: kotlin.Int): E
    public final fun removeFirst(): E
    public final fun removeFirstOrNull(): E?
    public final fun removeLast(): E
    public final fun removeLastOrNull(): E?
    public open override /*1*/ fun retainAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*1*/ fun set(/*0*/ index: kotlin.Int, /*1*/ element: E): E
}

public open class ArrayList</*0*/ E> : kotlin.collections.AbstractMutableList<E>, kotlin.collections.MutableList<E>, kotlin.collections.RandomAccess {
    public constructor ArrayList</*0*/ E>()
    public constructor ArrayList</*0*/ E>(/*0*/ initialCapacity: kotlin.Int = ...)
    public constructor ArrayList</*0*/ E>(/*0*/ elements: kotlin.collections.Collection<E>)
    public open override /*2*/ val size: kotlin.Int
        public open override /*2*/ fun <get-size>(): kotlin.Int
    public open override /*2*/ fun add(/*0*/ element: E): kotlin.Boolean
    public open override /*2*/ fun add(/*0*/ index: kotlin.Int, /*1*/ element: E): kotlin.Unit
    public open override /*2*/ fun addAll(/*0*/ index: kotlin.Int, /*1*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*2*/ fun addAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public open override /*2*/ fun clear(): kotlin.Unit
    public final fun ensureCapacity(/*0*/ minCapacity: kotlin.Int): kotlin.Unit
    public open override /*2*/ fun get(/*0*/ index: kotlin.Int): E
    public open override /*2*/ fun indexOf(/*0*/ element: E): kotlin.Int
    public open override /*2*/ fun lastIndexOf(/*0*/ element: E): kotlin.Int
    public open override /*2*/ fun remove(/*0*/ element: E): kotlin.Boolean
    public open override /*2*/ fun removeAt(/*0*/ index: kotlin.Int): E
    protected open override /*1*/ fun removeRange(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.Unit
    public open override /*2*/ fun set(/*0*/ index: kotlin.Int, /*1*/ element: E): E
    protected open override /*1*/ fun toArray(): kotlin.Array<kotlin.Any?>
    public open override /*3*/ fun toString(): kotlin.String
    public final fun trimToSize(): kotlin.Unit
}

public abstract class BooleanIterator : kotlin.collections.Iterator<kotlin.Boolean> {
    /*primary*/ public constructor BooleanIterator()
    public final override /*1*/ fun next(): kotlin.Boolean
    public abstract fun nextBoolean(): kotlin.Boolean
}

public abstract class ByteIterator : kotlin.collections.Iterator<kotlin.Byte> {
    /*primary*/ public constructor ByteIterator()
    public final override /*1*/ fun next(): kotlin.Byte
    public abstract fun nextByte(): kotlin.Byte
}

public abstract class CharIterator : kotlin.collections.Iterator<kotlin.Char> {
    /*primary*/ public constructor CharIterator()
    public final override /*1*/ fun next(): kotlin.Char
    public abstract fun nextChar(): kotlin.Char
}

public interface Collection</*0*/ out E> : kotlin.collections.Iterable<E> {
    public abstract val size: kotlin.Int
        public abstract fun <get-size>(): kotlin.Int
    public abstract operator fun contains(/*0*/ element: E): kotlin.Boolean
    public abstract fun containsAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract fun isEmpty(): kotlin.Boolean
    public abstract override /*1*/ fun iterator(): kotlin.collections.Iterator<E>
}

public abstract class DoubleIterator : kotlin.collections.Iterator<kotlin.Double> {
    /*primary*/ public constructor DoubleIterator()
    public final override /*1*/ fun next(): kotlin.Double
    public abstract fun nextDouble(): kotlin.Double
}

public abstract class FloatIterator : kotlin.collections.Iterator<kotlin.Float> {
    /*primary*/ public constructor FloatIterator()
    public final override /*1*/ fun next(): kotlin.Float
    public abstract fun nextFloat(): kotlin.Float
}

@kotlin.SinceKotlin(version = "1.1") public interface Grouping</*0*/ T, /*1*/ out K> {
    public abstract fun keyOf(/*0*/ element: T): K
    public abstract fun sourceIterator(): kotlin.collections.Iterator<T>
}

public open class HashMap</*0*/ K, /*1*/ V> : kotlin.collections.AbstractMutableMap<K, V>, kotlin.collections.MutableMap<K, V> {
    public constructor HashMap</*0*/ K, /*1*/ V>()
    public constructor HashMap</*0*/ K, /*1*/ V>(/*0*/ initialCapacity: kotlin.Int)
    public constructor HashMap</*0*/ K, /*1*/ V>(/*0*/ initialCapacity: kotlin.Int, /*1*/ loadFactor: kotlin.Float = ...)
    public constructor HashMap</*0*/ K, /*1*/ V>(/*0*/ original: kotlin.collections.Map<out K, V>)
    public open override /*2*/ val entries: kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
        public open override /*2*/ fun <get-entries>(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
    public open override /*2*/ val size: kotlin.Int
        public open override /*2*/ fun <get-size>(): kotlin.Int
    public open override /*2*/ fun clear(): kotlin.Unit
    public open override /*2*/ fun containsKey(/*0*/ key: K): kotlin.Boolean
    public open override /*2*/ fun containsValue(/*0*/ value: V): kotlin.Boolean
    protected open fun createEntrySet(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
    public open override /*2*/ fun get(/*0*/ key: K): V?
    public open override /*2*/ fun put(/*0*/ key: K, /*1*/ value: V): V?
    public open override /*2*/ fun remove(/*0*/ key: K): V?
}

public open class HashSet</*0*/ E> : kotlin.collections.AbstractMutableSet<E>, kotlin.collections.MutableSet<E> {
    public constructor HashSet</*0*/ E>()
    public constructor HashSet</*0*/ E>(/*0*/ initialCapacity: kotlin.Int)
    public constructor HashSet</*0*/ E>(/*0*/ initialCapacity: kotlin.Int, /*1*/ loadFactor: kotlin.Float = ...)
    public constructor HashSet</*0*/ E>(/*0*/ elements: kotlin.collections.Collection<E>)
    public open override /*2*/ val size: kotlin.Int
        public open override /*2*/ fun <get-size>(): kotlin.Int
    public open override /*2*/ fun add(/*0*/ element: E): kotlin.Boolean
    public open override /*2*/ fun clear(): kotlin.Unit
    public open override /*2*/ fun contains(/*0*/ element: E): kotlin.Boolean
    public open override /*2*/ fun isEmpty(): kotlin.Boolean
    public open override /*2*/ fun iterator(): kotlin.collections.MutableIterator<E>
    public open override /*2*/ fun remove(/*0*/ element: E): kotlin.Boolean
}

public final data class IndexedValue</*0*/ out T> {
    /*primary*/ public constructor IndexedValue</*0*/ out T>(/*0*/ index: kotlin.Int, /*1*/ value: T)
    public final val index: kotlin.Int
        public final fun <get-index>(): kotlin.Int
    public final val value: T
        public final fun <get-value>(): T
    public final operator /*synthesized*/ fun component1(): kotlin.Int
    public final operator /*synthesized*/ fun component2(): T
    public final /*synthesized*/ fun copy(/*0*/ index: kotlin.Int = ..., /*1*/ value: T = ...): kotlin.collections.IndexedValue<T>
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

public abstract class IntIterator : kotlin.collections.Iterator<kotlin.Int> {
    /*primary*/ public constructor IntIterator()
    public final override /*1*/ fun next(): kotlin.Int
    public abstract fun nextInt(): kotlin.Int
}

public interface Iterable</*0*/ out T> {
    public abstract operator fun iterator(): kotlin.collections.Iterator<T>
}

public interface Iterator</*0*/ out T> {
    public abstract operator fun hasNext(): kotlin.Boolean
    public abstract operator fun next(): T
}

public open class LinkedHashMap</*0*/ K, /*1*/ V> : kotlin.collections.HashMap<K, V>, kotlin.collections.MutableMap<K, V> {
    public constructor LinkedHashMap</*0*/ K, /*1*/ V>()
    public constructor LinkedHashMap</*0*/ K, /*1*/ V>(/*0*/ initialCapacity: kotlin.Int)
    public constructor LinkedHashMap</*0*/ K, /*1*/ V>(/*0*/ initialCapacity: kotlin.Int, /*1*/ loadFactor: kotlin.Float = ...)
    public constructor LinkedHashMap</*0*/ K, /*1*/ V>(/*0*/ original: kotlin.collections.Map<out K, V>)
    public open override /*2*/ val size: kotlin.Int
        public open override /*2*/ fun <get-size>(): kotlin.Int
    public open override /*2*/ fun clear(): kotlin.Unit
    public open override /*2*/ fun containsKey(/*0*/ key: K): kotlin.Boolean
    public open override /*2*/ fun containsValue(/*0*/ value: V): kotlin.Boolean
    protected open override /*1*/ fun createEntrySet(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
    public open override /*2*/ fun get(/*0*/ key: K): V?
    public open override /*2*/ fun put(/*0*/ key: K, /*1*/ value: V): V?
    public open override /*2*/ fun remove(/*0*/ key: K): V?
}

public open class LinkedHashSet</*0*/ E> : kotlin.collections.HashSet<E>, kotlin.collections.MutableSet<E> {
    public constructor LinkedHashSet</*0*/ E>()
    public constructor LinkedHashSet</*0*/ E>(/*0*/ initialCapacity: kotlin.Int)
    public constructor LinkedHashSet</*0*/ E>(/*0*/ initialCapacity: kotlin.Int, /*1*/ loadFactor: kotlin.Float = ...)
    public constructor LinkedHashSet</*0*/ E>(/*0*/ elements: kotlin.collections.Collection<E>)
}

public interface List</*0*/ out E> : kotlin.collections.Collection<E> {
    public abstract override /*1*/ val size: kotlin.Int
        public abstract override /*1*/ fun <get-size>(): kotlin.Int
    public abstract override /*1*/ fun contains(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract operator fun get(/*0*/ index: kotlin.Int): E
    public abstract fun indexOf(/*0*/ element: E): kotlin.Int
    public abstract override /*1*/ fun isEmpty(): kotlin.Boolean
    public abstract override /*1*/ fun iterator(): kotlin.collections.Iterator<E>
    public abstract fun lastIndexOf(/*0*/ element: E): kotlin.Int
    public abstract fun listIterator(): kotlin.collections.ListIterator<E>
    public abstract fun listIterator(/*0*/ index: kotlin.Int): kotlin.collections.ListIterator<E>
    public abstract fun subList(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.collections.List<E>
}

public interface ListIterator</*0*/ out T> : kotlin.collections.Iterator<T> {
    public abstract override /*1*/ fun hasNext(): kotlin.Boolean
    public abstract fun hasPrevious(): kotlin.Boolean
    public abstract override /*1*/ fun next(): T
    public abstract fun nextIndex(): kotlin.Int
    public abstract fun previous(): T
    public abstract fun previousIndex(): kotlin.Int
}

public abstract class LongIterator : kotlin.collections.Iterator<kotlin.Long> {
    /*primary*/ public constructor LongIterator()
    public final override /*1*/ fun next(): kotlin.Long
    public abstract fun nextLong(): kotlin.Long
}

public interface Map</*0*/ K, /*1*/ out V> {
    public abstract val entries: kotlin.collections.Set<kotlin.collections.Map.Entry<K, V>>
        public abstract fun <get-entries>(): kotlin.collections.Set<kotlin.collections.Map.Entry<K, V>>
    public abstract val keys: kotlin.collections.Set<K>
        public abstract fun <get-keys>(): kotlin.collections.Set<K>
    public abstract val size: kotlin.Int
        public abstract fun <get-size>(): kotlin.Int
    public abstract val values: kotlin.collections.Collection<V>
        public abstract fun <get-values>(): kotlin.collections.Collection<V>
    public abstract fun containsKey(/*0*/ key: K): kotlin.Boolean
    public abstract fun containsValue(/*0*/ value: V): kotlin.Boolean
    public abstract operator fun get(/*0*/ key: K): V?
    public abstract fun isEmpty(): kotlin.Boolean

    public interface Entry</*0*/ out K, /*1*/ out V> {
        public abstract val key: K
            public abstract fun <get-key>(): K
        public abstract val value: V
            public abstract fun <get-value>(): V
    }
}

public interface MutableCollection</*0*/ E> : kotlin.collections.Collection<E>, kotlin.collections.MutableIterable<E> {
    public abstract fun add(/*0*/ element: E): kotlin.Boolean
    public abstract fun addAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract fun clear(): kotlin.Unit
    public abstract override /*2*/ fun iterator(): kotlin.collections.MutableIterator<E>
    public abstract fun remove(/*0*/ element: E): kotlin.Boolean
    public abstract fun removeAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract fun retainAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public interface MutableIterable</*0*/ out T> : kotlin.collections.Iterable<T> {
    public abstract override /*1*/ fun iterator(): kotlin.collections.MutableIterator<T>
}

public interface MutableIterator</*0*/ out T> : kotlin.collections.Iterator<T> {
    public abstract fun remove(): kotlin.Unit
}

public interface MutableList</*0*/ E> : kotlin.collections.List<E>, kotlin.collections.MutableCollection<E> {
    public abstract override /*1*/ fun add(/*0*/ element: E): kotlin.Boolean
    public abstract fun add(/*0*/ index: kotlin.Int, /*1*/ element: E): kotlin.Unit
    public abstract fun addAll(/*0*/ index: kotlin.Int, /*1*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun addAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun clear(): kotlin.Unit
    public abstract override /*1*/ fun listIterator(): kotlin.collections.MutableListIterator<E>
    public abstract override /*1*/ fun listIterator(/*0*/ index: kotlin.Int): kotlin.collections.MutableListIterator<E>
    public abstract override /*1*/ fun remove(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun removeAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract fun removeAt(/*0*/ index: kotlin.Int): E
    public abstract override /*1*/ fun retainAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract operator fun set(/*0*/ index: kotlin.Int, /*1*/ element: E): E
    public abstract override /*1*/ fun subList(/*0*/ fromIndex: kotlin.Int, /*1*/ toIndex: kotlin.Int): kotlin.collections.MutableList<E>
}

public interface MutableListIterator</*0*/ T> : kotlin.collections.ListIterator<T>, kotlin.collections.MutableIterator<T> {
    public abstract fun add(/*0*/ element: T): kotlin.Unit
    public abstract override /*2*/ fun hasNext(): kotlin.Boolean
    public abstract override /*2*/ fun next(): T
    public abstract override /*1*/ fun remove(): kotlin.Unit
    public abstract fun set(/*0*/ element: T): kotlin.Unit
}

public interface MutableMap</*0*/ K, /*1*/ V> : kotlin.collections.Map<K, V> {
    public abstract override /*1*/ val entries: kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
        public abstract override /*1*/ fun <get-entries>(): kotlin.collections.MutableSet<kotlin.collections.MutableMap.MutableEntry<K, V>>
    public abstract override /*1*/ val keys: kotlin.collections.MutableSet<K>
        public abstract override /*1*/ fun <get-keys>(): kotlin.collections.MutableSet<K>
    public abstract override /*1*/ val values: kotlin.collections.MutableCollection<V>
        public abstract override /*1*/ fun <get-values>(): kotlin.collections.MutableCollection<V>
    public abstract fun clear(): kotlin.Unit
    public abstract fun put(/*0*/ key: K, /*1*/ value: V): V?
    public abstract fun putAll(/*0*/ from: kotlin.collections.Map<out K, V>): kotlin.Unit
    public abstract fun remove(/*0*/ key: K): V?

    public interface MutableEntry</*0*/ K, /*1*/ V> : kotlin.collections.Map.Entry<K, V> {
        public abstract fun setValue(/*0*/ newValue: V): V
    }
}

public interface MutableSet</*0*/ E> : kotlin.collections.Set<E>, kotlin.collections.MutableCollection<E> {
    public abstract override /*1*/ fun add(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun addAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun clear(): kotlin.Unit
    public abstract override /*2*/ fun iterator(): kotlin.collections.MutableIterator<E>
    public abstract override /*1*/ fun remove(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun removeAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun retainAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
}

public interface RandomAccess {
}

public interface Set</*0*/ out E> : kotlin.collections.Collection<E> {
    public abstract override /*1*/ val size: kotlin.Int
        public abstract override /*1*/ fun <get-size>(): kotlin.Int
    public abstract override /*1*/ fun contains(/*0*/ element: E): kotlin.Boolean
    public abstract override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<E>): kotlin.Boolean
    public abstract override /*1*/ fun isEmpty(): kotlin.Boolean
    public abstract override /*1*/ fun iterator(): kotlin.collections.Iterator<E>
}

public abstract class ShortIterator : kotlin.collections.Iterator<kotlin.Short> {
    /*primary*/ public constructor ShortIterator()
    public final override /*1*/ fun next(): kotlin.Short
    public abstract fun nextShort(): kotlin.Short
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public abstract class UByteIterator : kotlin.collections.Iterator<kotlin.UByte> {
    /*primary*/ public constructor UByteIterator()
    public final override /*1*/ fun next(): kotlin.UByte
    public abstract fun nextUByte(): kotlin.UByte
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public abstract class UIntIterator : kotlin.collections.Iterator<kotlin.UInt> {
    /*primary*/ public constructor UIntIterator()
    public final override /*1*/ fun next(): kotlin.UInt
    public abstract fun nextUInt(): kotlin.UInt
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public abstract class ULongIterator : kotlin.collections.Iterator<kotlin.ULong> {
    /*primary*/ public constructor ULongIterator()
    public final override /*1*/ fun next(): kotlin.ULong
    public abstract fun nextULong(): kotlin.ULong
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public abstract class UShortIterator : kotlin.collections.Iterator<kotlin.UShort> {
    /*primary*/ public constructor UShortIterator()
    public final override /*1*/ fun next(): kotlin.UShort
    public abstract fun nextUShort(): kotlin.UShort
}
