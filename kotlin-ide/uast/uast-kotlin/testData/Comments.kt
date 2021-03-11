/**
 * Common Pizza Interface
 *
 * @see Margherita
 */
interface Pizza {
    /** The size of [Pizza] */
    val size: Int

    /**
     * Human-readable name of type [string_link_alias][java.lang.String]
     *
     * @throws java.lang.IllegalStateException
     * @throws ClassCastException
     */
    fun getName(): java.lang.String

    /**
     * Checks whether pizza contains the specified [Pizza.Ingredient]
     *
     * @param ingredient the ingredient to check
     * @exception kotlin.NotImplementedError
     * @sample Margherita.contains
     */
    fun contains(ingredient: Ingredient): Boolean

    /** Ingredient of [Pizza] */
    interface Ingredient

    /**
     * Abstract [Pizza] builder
     *
     * @param P well-known hack to solve abstract builder chain problem, see [self]
     */
    abstract class Builder<P: Pizza.Builder<P>> {
        private val ingredients: MutableSet<Pizza.Ingredient> = mutableSetOf()

        /**
         * Adds [ingredient] to the [ingredients]
         *
         * @return value of type [P]
         */
        abstract fun addIngredient(ingredient: Pizza.Ingredient): P
        abstract fun build(): Pizza
        protected abstract fun self(): P
    }
}

/**
 * Pizza Margherita
 *
 * @see Pizza
 * @property size ideal size of [Margherita] is of course 42
 */
class Margherita(override val size: Int = 42) : Pizza {
    override fun getName(): java.lang.String = java.lang.String("Margherita")

    /**
     * Checks whether pizza contains the specified [Pizza.Ingredient]
     *
     * @param ingredient see [Pizza.Ingredient]
     */
    override fun contains(ingredient: Pizza.Ingredient): Boolean = false
}