var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return Kotlin.isType(new foo.A, foo.A);
  }
}
}, {A:classes.A});
foo.initialize();
