// WITH_RUNTIME
fun foo(c : Collection<String>){
  c.filter<selection>{it; false}</selection>
}