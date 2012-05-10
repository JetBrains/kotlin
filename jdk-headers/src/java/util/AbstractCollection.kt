package java.util
public abstract class AbstractCollection<erased E> protected () : java.lang.Object(), java.util.Collection<E> {
    public abstract override fun iterator() : java.util.Iterator<E>
    public abstract override fun size() : Int
    public override fun isEmpty() : Boolean {}
    public override fun contains(o : Any?) : Boolean {}
    public override fun toArray() : Array<Any?> {}
    public override fun toArray<erased T>(a : Array<out T>) : Array<T> {}
    public override fun add(e : E) : Boolean {}
    public override fun remove(o : Any?) : Boolean {}
    public override fun containsAll(c : java.util.Collection<*>) : Boolean {}
    public override fun addAll(c : java.util.Collection<out E>) : Boolean {}
    public override fun removeAll(c : java.util.Collection<*>) : Boolean {}
    public override fun retainAll(c : java.util.Collection<*>) : Boolean {}
    public override fun clear() : Unit {}
//public override fun toString() : java.lang.String {}
}
