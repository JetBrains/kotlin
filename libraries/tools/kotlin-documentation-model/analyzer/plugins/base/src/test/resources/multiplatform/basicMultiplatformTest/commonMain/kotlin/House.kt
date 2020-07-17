package example

class House(val street: String, val number: Int) {

    /**
     * The owner of the house
     */
    var owner: String = ""

    /**
     * The owner of the house
     */
    val differentOwner: String = ""

    fun addFloor() {}

    class Basement {
        val pickles : List<Any> = mutableListOf()
    }

    companion object {
        val DEFAULT = House("",0)
    }
}