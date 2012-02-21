var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var arr = new Kotlin.ArrayList;
    var i = 0;
    while (i++ < 10) {
      arr.add(i);
    }
    return arr.get(10) == 11;
  }
}
}, {});
foo.initialize();
