var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var a = 3;
    var b = a++;
    a--;
    a--;
    return a++ == 2 && b == 3;
  }
}
}, {});
foo.initialize();
