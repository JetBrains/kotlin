var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$x = 4;
  }
  , get_x:function(){
    return this.$x;
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
    return (tmp$0 = a , tmp$0 != null?tmp$0.get_x():null) == null;
  }
}
}, {A:classes.A});
foo.initialize();
