package test

import kotlinApi.KotlinClassAbstractProperty

class KotlinClassAbstractPropertyImpl : KotlinClassAbstractProperty() {
    override var isVisible = false
        private set

    private fun test() {
        isVisible = true
    }
}
