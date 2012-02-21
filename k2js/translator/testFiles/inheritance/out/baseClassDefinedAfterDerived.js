var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 3;
  }
  , get_a:function(){
    return this.$a;
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  });
  return {A:tmp$1, B:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).get_a() == 3;
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
