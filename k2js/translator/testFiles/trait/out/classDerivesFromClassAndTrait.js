var classes = function(){
  var tmp$0 = Kotlin.Trait.create({addFoo:function(s){
    {
      return s + 'FOO';
    }
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(){
    this.$value = 'BAR';
  }
  , get_value:function(){
    return this.$value;
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, tmp$0, {initialize:function(){
    this.super_init();
  }
  , eval_0:function(){
    {
      return this.addFoo(this.get_value());
    }
  }
  });
  return {A:tmp$1, B:tmp$2, Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.B).eval_0() == 'BARFOO';
  }
}
}, {A:classes.A, Test:classes.Test, B:classes.B});
foo.initialize();
