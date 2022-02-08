// org.junit.Assert.assertThrows was added in 4.13, compilation should be fine
@file:DependsOn("junit:junit:(4.12,5.0)")

org.junit.Assert.assertThrows(NullPointerException::class.java) {
    throw null!!
}

println("Hello, World!")

