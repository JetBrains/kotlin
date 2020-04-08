fun test() {
  val a : Int? = 0
  if (a != null) {
    a.plus(1)
  }
  else {
    a?.plus(1)
  }

  val out : java.io.PrintStream? = null
  val ins : java.io.InputStream? = null

  out?.println()
  ins?.read()

  if (ins != null) {
    ins.read()
    out?.println()
    if (out != null) {
      ins.read();
      out.println();
    }
  }

  if (out != null && ins != null) {
    ins.read();
    out.println();
  }

  if (out == null) {
    out?.println()
  } else {
    out.println()
  }

  if (out != null && ins != null || out != null) {
    ins?.read();
    out.println();
  }

  if (out == null || out.println(0) == Unit) {
    out?.println(1)
  }
  else {
    out.println(2)
  }

  if (out != null && out.println() == Unit) {
    out.println();
  }
  else {
    out?.println();
  }

  if (out == null || out.println() == Unit) {
    out?.println();
  }
  else {
    out.println();
  }

  if (1 == 2 || out != null && out.println(1) == Unit) {
    out?.println(2);
  }
  else {
    out?.println(3)
  }

  out?.println()
  ins?.read()

  if (ins != null) {
    ins.read()
    out?.println()
    if (out != null) {
      ins.read();
      out.println();
    }
  }

  if (out != null && ins != null) {
    ins.read();
    out.println();
  }

  if (out == null) {
    out?.println()
  } else {
    out.println()
  }

  if (out != null && ins != null || out != null) {
    ins?.read();
    out.println();
  }

  if (out == null || out.println(0) == Unit) {
    out?.println(1)
  }
  else {
    out.println(2)
  }

  if (out != null && out.println() == Unit) {
    out.println();
  }
  else {
    out?.println();
  }

  if (out == null || out.println() == Unit) {
    out?.println();
  }
  else {
    out.println();
  }

  if (1 == 2 || out != null && out.println(1) == Unit) {
    out?.println(2);
  }
  else {
    out?.println(3)
  }

  if (1 > 2) {
    if (out == null) return;
    out.println();
  }
  out?.println();

  while (out != null) {
    out.println();
  }
  out?.println();

  val out2 : java.io.PrintStream? = null
  
  while (out2 == null) {
    out2?.println();
  }
  out2.println()

}


fun f(out : String?) {
  out?.get(0)
  if (out != null) else return;
  out.get(0)
}

fun f1(out : String?) {
  out?.get(0)
  if (out != null) else {
    1 + 2
    return;
  }
  out.get(0)
}

fun f2(out : String?) {
  out?.get(0)
  if (out == null) {
    1 + 2
    return;
  }
  out.get(0)
}

fun f3(out : String?) {
  out?.get(0)
  if (out == null) {
    1 + 2
    return;
  }
  else {
    1 + 2
  }
  out.get(0)
}

fun f4(s : String?) {
  s?.get(0)
  while (1 < 2 && s != null) {
    s.get(0)
  }
  s?.get(0)
  while (s == null || 1 < 2) {
     s?.get(0)
  }
  s.get(0)
}

fun f5(s : String?) {
  s?.get(0)
  while (1 < 2 && s != null) {
    s.get(0)
  }
  s?.get(0)
  while (s == null || 1 < 2) {
    if (1 > 2) break
     s?.get(0)
  }
  s?.get(0);
}

fun f6(s : String?) {
  s?.get(0)
  do {
    s?.get(0)
    if (1 < 2) break;
  } while (s == null)
  s?.get(0)
  do {
    s?.get(0)
  } while (s == null)
  s.get(0)
}

fun f7(s : String?, t : String?) {
  s?.get(0)
  if (!(s == null)) {
    s.get(0)
  }
  s?.get(0)
  if (!(s != null)) {
    s?.get(0)
  }
  else {
    s.get(0)
  }
  s?.get(0)
  if (!!(s != null)) {
    s.get(0)
  }
  else {
    s?.get(0)
  }
  s?.get(0)
  t?.get(0)
  if (!(s == null || t == null)) {
    s.get(0)
    t.get(0)
  }
  else {
    s?.get(0)
    t?.get(0)
  }
  s?.get(0)
  t?.get(0)
  if (!(s == null)) {
    s.get(0)
    t?.get(0)
  }
  else {
    s?.get(0)
    t?.get(0)
  }
}

fun f8(b : String?, a : String) {
  b?.get(0)
  if (b == a) {
    b.get(0);
  }
  b?.get(0)
  if (a == b) {
    b.get(0)
  }
  if (a != b) {
    b?.get(0)
  }
  else {
    b.get(0)
  }
}

fun f9(a : Int?) : Int {
  if (a != null)
    return a
  return 1
}
