package kotlin2

public trait ListLike<T>: CollectionLike<T> {

    // list like API
    public abstract fun get(index: Int): T

    /**
    * Get the first element in the collection.
    *
    * Will throw an exception if there are no elements
    */
    public override fun first(): T {
        return get(0)
    }

    public override fun last(): T {
        return get(this.size() - 1);
    }

}