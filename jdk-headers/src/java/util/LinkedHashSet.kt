package java.util
public open class LinkedHashSet<erased E>(c : java.util.Collection<out E>) : java.util.HashSet<E>(c),
                                                                             java.util.Set<E>,
                                                                             java.lang.Cloneable,
                                                                             java.io.Serializable {
    public this(initialCapacity : Int, loadFactor : Float) {}
    public this(initialCapacity : Int) {}
    public this() {}
}