var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a1 = Kotlin.arrayFromFun(3, function(i){
    {
      return i;
    }
  }
  );
}
, get_a1:function(){
  return $a1;
}
, box:function(){
  {
    var i = Kotlin.arrayIterator(foo.get_a1());
    return i.get_hasNext() == true && i.next() == 0 && i.get_hasNext() == true && i.next() == 1 && i.get_hasNext() == true && i.next() == 2 && i.get_hasNext() == false;
  }
}
}, {});
foo.initialize();
