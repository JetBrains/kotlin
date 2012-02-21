var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 3;
  }
  , get_a:function(){
    return this.$a;
  }
  , set_a:function(tmp$0){
    this.$a = tmp$0;
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
    return (new foo.B).get_a();
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
