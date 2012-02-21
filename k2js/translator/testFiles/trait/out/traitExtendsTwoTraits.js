var classes = function(){
  var tmp$0 = Kotlin.Trait.create({hooray:function(){
    {
      return 'hooray';
    }
  }
  });
  var tmp$1 = Kotlin.Trait.create({addFoo:function(s){
    {
      return s + 'FOO';
    }
  }
  });
  var tmp$2 = Kotlin.Trait.create(tmp$1, tmp$0, {});
  var tmp$3 = Kotlin.Class.create(tmp$2, {initialize:function(){
  }
  , eval_0:function(){
    {
      return this.addFoo(this.hooray());
    }
  }
  });
  return {A:tmp$1, AD:tmp$2, Test:tmp$3, B:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.Test).eval_0() == 'hoorayFOO';
  }
}
}, {A:classes.A, B:classes.B, AD:classes.AD, Test:classes.Test});
foo.initialize();
