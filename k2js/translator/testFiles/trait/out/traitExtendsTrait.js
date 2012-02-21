var classes = function(){
  var tmp$0 = Kotlin.Trait.create({addFoo:function(s){
    {
      return s + 'FOO';
    }
  }
  });
  var tmp$1 = Kotlin.Trait.create(tmp$0, {hooray:function(){
    {
      return 'hooray';
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, {initialize:function(){
  }
  , eval_0:function(){
    {
      return this.addFoo(this.hooray());
    }
  }
  });
  return {ExtendedTest:tmp$1, A:tmp$2, Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).eval_0() == 'hoorayFOO';
  }
}
}, {Test:classes.Test, ExtendedTest:classes.ExtendedTest, A:classes.A});
foo.initialize();
