var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var a = new Kotlin.ArrayList;
    a.add(1);
    a.add(2);
    return a.size() == 2 && a.get(1) == 2 && a.get(0) == 1;
  }
}
}, {});
foo.initialize();
