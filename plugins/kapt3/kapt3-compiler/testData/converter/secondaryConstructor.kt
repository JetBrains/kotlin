// CORRECT_ERROR_TYPES

package secondary

interface Named {
    val name: String?
}

class Product2 : Named {
    override var name: String? = null

    constructor(otherName: String) {
        this.name = otherName
    }
}