package test

class EnumWithQuotes {
    internal class B {
        private enum class Zoo {
            Bear, `Bear+`, `Polar Bear`, Panda
        }
    }
}