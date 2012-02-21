var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$b = 2;
  }
  , get_b:function(){
    return this.$b;
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(){
    this.$a = 1;
  }
  , get_a:function(){
    return this.$a;
  }
  });
  return {A:tmp$1, B:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).get_a() == 1 && (new foo.B).get_b() == 2;
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
