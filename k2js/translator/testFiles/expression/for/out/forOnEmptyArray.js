var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a1 = Kotlin.nullArray(0);
}
, get_a1:function(){
  return $a1;
}
, box:function(){
  {
    var tmp$0;
    {
      tmp$0 = Kotlin.arrayIterator(foo.get_a1());
      while (tmp$0.hasNext()) {
        var a = tmp$0.next();
        {
          return false;
        }
      }
    }
    return true;
  }
}
}, {});
foo.initialize();
