fun box() = expectThrowableMessage {
    val prefixValue = "Hello:"
    val str = """
        This
         Is
          A
           Long
          Multiple
         Line
        String
    """.trimIndent()
    assert((prefixValue + str.substring(0, 8)).length == 0)
}
