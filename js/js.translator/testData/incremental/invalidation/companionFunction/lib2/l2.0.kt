inline fun makeMyInterfaceObject(x: Boolean): MyInterface {
    return if (x) {
        object : MyInterface {
            override fun interfaceFunction(): String {
                return MyClass1.companionFunction().toString()
            }
        }
    } else {
        object : MyInterface {
            override fun interfaceFunction(): String {
                return MyClass2.companionFunction()
            }
        }
    }
}
