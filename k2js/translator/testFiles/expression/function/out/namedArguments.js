var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, test:function(x, y){
  {
    return y - x;
  }
}
, box:function(){
  {
    if (foo.test(1, 2) != 1) {
      return false;
    }
    if (foo.test(1, 2) != 1) {
      return false;
    }
    if (foo.test(1, 2) != 1) {
      return false;
    }
    return true;
  }
}
}, {});
foo.initialize();
