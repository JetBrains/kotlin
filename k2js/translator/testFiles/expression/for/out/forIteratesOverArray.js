var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a1 = Kotlin.nullArray(10);
}
, get_a1:function(){
  return $a1;
}
, box:function(){
  {
    var tmp$0;
    var c = 0;
    var d = 0;
    foo.get_a1()[3] = 3;
    foo.get_a1()[5] = 5;
    {
      tmp$0 = Kotlin.arrayIterator(foo.get_a1());
      while (tmp$0.hasNext()) {
        var a = tmp$0.next();
        {
          if (a != null) {
            c += 1;
          }
           else {
            d += 1;
          }
        }
      }
    }
    return c == 2 && d == 8;
  }
}
}, {});
foo.initialize();
