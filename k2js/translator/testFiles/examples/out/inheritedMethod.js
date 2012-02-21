var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , xyzzy:function(){
    {
      return 'xyzzy';
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  , test:function(){
    {
      return this.xyzzy();
    }
  }
  });
  return {Bar:tmp$1, Foo:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var bar = new Anonymous.Bar;
    var f = bar.test();
    if (f == 'xyzzy')
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
}, {Foo:classes.Foo, Bar:classes.Bar});
Anonymous.initialize();
