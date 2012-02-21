var classes = function(){
  return {};
}
();
var bar = Kotlin.Namespace.create({initialize:function(){
}
, f:function(){
  {
    return 3;
  }
}
}, {});
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return bar.f() == 3;
  }
}
}, {});
bar.initialize();
foo.initialize();
