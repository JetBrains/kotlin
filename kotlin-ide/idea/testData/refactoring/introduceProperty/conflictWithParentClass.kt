// EXTRACTION_TARGET: property with initializer
open class Base(protected val i: Int)

class Impl(z: Int) : Base(z) {
    fun foo(): Int {
        return <selection>2 + 3 + i</selection>
    }
}