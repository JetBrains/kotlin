var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , f:function(){
    {
      return 3;
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  , f:function(){
    {
      return 4;
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, {initialize:function(){
    this.super_init();
  }
  , f:function(){
    {
      return 5;
    }
  }
  });
  return {B:tmp$1, C:tmp$2, A:tmp$0};
}
();
var bar = Kotlin.Namespace.create({initialize:function(){
}
}, {B:classes.B});
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).f() == 3 && (new bar.B).f() == 4 && (new foo.C).f() == 5;
  }
}
}, {A:classes.A, C:classes.C});
bar.initialize();
foo.initialize();
