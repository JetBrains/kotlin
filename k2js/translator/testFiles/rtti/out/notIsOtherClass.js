var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  });
  return {B:tmp$1, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return !Kotlin.isType(new foo.A, foo.B);
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
