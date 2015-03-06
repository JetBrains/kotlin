package demo

class Container {
    var myBoolean = true
}

class One {
    default object {
        var myContainer = Container()
    }
}

class Test {
    fun test() {
        if (One.myContainer.myBoolean)
            System.out.println("Ok")

        val s = if (One.myContainer.myBoolean) "YES" else "NO"

        while (One.myContainer.myBoolean)
            System.out.println("Ok")

        do {
            System.out.println("Ok")
        } while (One.myContainer.myBoolean)
    }
}