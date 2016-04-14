package test

import kotlinApi.*

class KotlinClassAbstractPropertyImpl : KotlinClassAbstractProperty() {
    override var isVisible: Boolean = false
        private set

    private fun test() {
        isVisible = true
    }
}
