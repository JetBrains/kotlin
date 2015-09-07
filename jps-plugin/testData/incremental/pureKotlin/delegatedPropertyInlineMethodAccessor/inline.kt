package inline

class Inline {
    inline fun get(receiver: Any?, prop: PropertyMetadata): Int {
        return 0
    }

    inline fun set(receiver: Any?, prop: PropertyMetadata, value: Int) {
        println(value)
    }
}