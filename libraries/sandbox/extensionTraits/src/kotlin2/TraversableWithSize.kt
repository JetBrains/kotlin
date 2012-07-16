package kotlin2

/**
 */
public trait TraversableWithSize<T>: Traversable<T> {
    public abstract fun size(): Int


    /**
    * Count the number of elements in collection.
    *
    * If base collection implements [[Collection]] interface method [[Collection.size()]] will be used.
    * Otherwise, this method determines the count by iterating through the all items.
    */
    public override fun count(): Int {
        return size()
    }
}
