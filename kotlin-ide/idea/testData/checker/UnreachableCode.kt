fun t1() : Int{
  return 0
  <warning>1</warning>
}

fun t1a() : Int {
  <error>return</error>
  <warning>return 1</warning>
  <warning>1</warning>
}

fun t1b() : Int {
  return 1
  <warning>return 1</warning>
  <warning>1</warning>
}

fun t1c() : Int {
  return 1
  <error>return</error>
  <warning>1</warning>
}

fun t2() : Int {
  if (1 > 2)
    return 1
  else return 1
  <warning>1</warning>
}

fun t2a() : Int {
  if (1 > 2) {
    return 1
    <warning>1</warning>
  } else { return 1
    <warning>2</warning>
  }
  <warning>1</warning>
}

fun t3() : Any {
  if (1 > 2)
    return 2
  else return ""
  <warning>1</warning>
}

fun t4(<warning>a</warning> : Boolean) : Int {
  do {
    return 1
  }
  while (<warning>a</warning>)
  <warning>1</warning>
}

fun t4break(<warning>a</warning> : Boolean) : Int {
  do {
    break
  }
  while (<warning>a</warning>)
  return 1
}

fun t5() : Int {
  do {
    return 1
    <warning>2</warning>
  }
  while (<warning>1 > 2</warning>)
  <warning>return 1</warning>
}

fun t6() : Int {
  while (1 > 2) {
    return 1
    <warning>2</warning>
  }
  return 1
}

fun t6break() : Int {
  while (1 > 2) {
    break
    <warning>2</warning>
  }
  return 1
}

fun t7(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <warning>2</warning>
  }
  return 1
}

fun t7break(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <warning>2</warning>
  }
  return 1
}

fun t7() : Int {
  try {
    return 1
    <warning>2</warning>
  }
  catch (<error>e : Any</error>) {
    <warning>2</warning>
  }
  return 1 // this is OK, like in Java
}

fun t8() : Int {
  try {
    return 1
    <warning>2</warning>
  }
  catch (<error>e : Any</error>) {
    return 1
    <warning>2</warning>
  }
  <warning>return 1</warning>
}

fun blockAndAndMismatch() : Boolean {
  (return true) <warning>|| (return false)</warning>
  <warning>return true</warning>
}

fun tf() : Int {
  try {<warning>return</warning> 1} finally{return 1}
  <warning>return 1</warning>
}

fun failtest(<warning>a</warning> : Int) : Int {
  if (fail() <warning>|| true</warning>) <warning>{

  }</warning>
  <warning>return 1</warning>
}

fun foo(a : Nothing) : Unit {
  <warning>1</warning>
  <warning>a</warning>
  <warning>2</warning>
}

fun fail() : Nothing {
  throw java.lang.RuntimeException()
}

fun nullIsNotNothing() : Unit {
    val x : Int? = 1
    if (x != null) {
         return
    }
    fail()
}