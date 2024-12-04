package test

class Counter {
    suspend fun one() {}
    suspend fun two() {}
    suspend fun both() {
        one()
        two()
    }
}
