var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , f:function(){
    {
      return 42;
    }
  }
  });
  var tmp$1 = Kotlin.Trait.create({});
  var tmp$2 = Kotlin.Class.create(tmp$0, tmp$1, {initialize:function(){
    this.super_init();
  }
  , f:function(){
    {
      return 239;
    }
  }
  });
  return {Left:tmp$1, D:tmp$2, Right:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var r = new Anonymous.Right;
    var d = new Anonymous.D;
    if (r.f() != 42)
      return 'Fail #1';
    if (d.f() != 239)
      return 'Fail #2';
    return 'OK';
  }
}
}, {Left:classes.Left, Right:classes.Right, D:classes.D});
Anonymous.initialize();
