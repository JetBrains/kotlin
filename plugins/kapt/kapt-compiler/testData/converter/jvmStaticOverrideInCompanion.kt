// WITH_STDLIB

abstract class ClientFactory<B> {
    abstract fun builder(): B
}

class Client {
    class Builder

    companion object : ClientFactory<Builder>() {
        @JvmStatic
        override fun builder(): Builder = Builder()
    }
}
