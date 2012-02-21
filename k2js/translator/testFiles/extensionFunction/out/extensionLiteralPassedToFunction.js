var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, apply:function(i, f){
  {
    return f.call(i, 1);
  }
}
, box:function(){
  {
    return foo.apply(1, function(i){
      {
        return i + this;
      }
    }
    ) == 2;
  }
}
}, {});
foo.initialize();
