var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , f:function(){
    {
      return 'C f';
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  , f:function(){
    {
      return 'D f';
    }
  }
  });
  return {D:tmp$1, C:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var d = new foo.D;
    if (d.f() != 'D f')
      return false;
    return true;
  }
}
}, {C:classes.C, D:classes.D});
foo.initialize();
