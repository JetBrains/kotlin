fun testFunction() {
}

class Some {
  fun testFunInClass() = 12
}

// SEARCH_TEXT: test
// REF: (<root>).testFunction()
// REF: (in Some).testFunInClass()


