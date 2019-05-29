package test

import kotlinApi.*

class KotlinClassAbstractPropertyImpl : KotlinClassAbstractProperty() {
    override var isVisible = false
        private set

    private fun test() {
        isVisible = true
    }
}