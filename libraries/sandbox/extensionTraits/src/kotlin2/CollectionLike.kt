package kotlin2

public trait CollectionLike<T>: TraversableWithSize<T>, EagerTraversable<T> {

    public override abstract fun contains(item: T): Boolean

}
