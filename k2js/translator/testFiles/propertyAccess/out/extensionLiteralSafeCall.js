var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(a, b){
  var tmp$0;
  {
    return tmp$0 = a , tmp$0 != null?b.call(tmp$0, 2):null;
  }
}
, box:function(){
  {
    var c1 = foo.f(null, function(it){
      {
        return it + this;
      }
    }
    ) != null;
    if (c1)
      return false;
    if (foo.f(3, function(it){
      {
        return it + this;
      }
    }
    ) != 5)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
