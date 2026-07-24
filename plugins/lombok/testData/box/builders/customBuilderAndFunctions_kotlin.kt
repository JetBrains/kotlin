import lombok.Builder

@Builder(toBuilder = true, builderClassName = "MyClassBuilder")
class MyClass(val aString: String) {
    companion object {
        @JvmStatic
        fun builder(x: Int): MyClassBuilder {
            return CustomMyClassBuilder()
        }
    }

    open class MyClassBuilder {
        companion object {
            var myStaticField = 42
        }
    }

    // Make sure Lombok ignores the custom unrelated class
    class MyCustomClass

    class CustomMyClassBuilder : MyClassBuilder() {
        override fun build(): MyClass {
            myStaticField = 100
            return super.build()
        }
    }
}

fun box(): String {
    MyClass.MyClassBuilder.myStaticField = 84

    val myClassBuilder: MyClass.MyClassBuilder = MyClass.builder(0)
    val myClass = myClassBuilder.aString("test").build()

    return if (myClassBuilder is MyClass.CustomMyClassBuilder && // Check if custom `builder` method is called
        MyClass.MyClassBuilder.myStaticField == 100 && // Check if custom `build` method is called
        myClass.aString == "test"
    ) {
        "OK"
    } else {
        "Error: $myClass"
    }
}
