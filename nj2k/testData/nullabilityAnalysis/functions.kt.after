fun notNullParameters(f: Function2<Int, Int, String>) {}
fun nullableParameter(f: Function2<Int?, Int, String>) {}
fun nullableReturnType(f: Function2<Int, Int, String?>) {}


fun test() {
    notNullParameters({ i: Int, j: Int ->
                          if (i < 10 && j > 0) "" else ""
                      })

    nullableParameter({ i: Int?, j: Int ->
                          if (i == null) "" else ""
                      })

    nullableReturnType({ i: Int, j: Int ->
                           if (i < 10) return@nullableReturnType null
                           return@nullableReturnType "nya"
                       })
}
