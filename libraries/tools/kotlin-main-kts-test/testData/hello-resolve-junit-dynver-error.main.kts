// // org.junit.Assert.assertThrows was added in 4.13, compilation should fail
@file:DependsOn("junit:junit:(4.11,4.12]")

org.junit.Assert.assertThrows(NullPointerException::class.java) {
    throw null!!
}

println("Hello, World!")

