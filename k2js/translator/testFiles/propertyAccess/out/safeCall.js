var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , doSomething:function(){
    {
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var a = null;
    tmp$0 = a , tmp$0 != null?tmp$0.doSomething():null;
    return true;
  }
}
}, {A:classes.A});
foo.initialize();
