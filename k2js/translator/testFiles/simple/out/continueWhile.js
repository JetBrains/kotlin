var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var i = 0;
    var b = true;
    while (i < 100) {
      ++i;
      if (i >= 1) {
        continue;
      }
      b = false;
    }
    return b;
  }
}
}, {});
foo.initialize();
