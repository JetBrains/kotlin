package test

class SomeClass {
    private interface PrivateInterface {
        fun d() = "Default implementation of removed interface should not affect abi."
    }
}
