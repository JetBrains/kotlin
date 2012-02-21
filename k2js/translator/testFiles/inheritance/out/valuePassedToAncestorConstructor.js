var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a){
    this.$b = a;
  }
  , get_b:function(){
    return this.$b;
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(c){
    this.super_init(c + 2);
  }
  });
  return {D:tmp$1, C:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.D(0)).get_b() == 2;
  }
}
}, {C:classes.C, D:classes.D});
foo.initialize();
