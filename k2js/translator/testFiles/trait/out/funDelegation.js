var classes = function(){
  var tmp$0 = Kotlin.Trait.create({});
  var tmp$1 = Kotlin.Class.create({initialize:function(){
  }
  , n_0:function(n){
    {
      return n + 1;
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, tmp$0, {initialize:function(){
    this.super_init();
  }
  });
  var tmp$3 = Kotlin.Class.create(tmp$1, tmp$0, {initialize:function(){
    this.super_init();
  }
  });
  return {Base:tmp$1, Derived2:tmp$2, Derived1:tmp$3, Abstract:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, test:function(s){
  {
    return s.n_0(238) == 239;
  }
}
, box:function(){
  {
    if (!foo.test(new foo.Base))
      return 'Fail #1';
    if (!foo.test(new foo.Derived1))
      return 'Fail #2';
    if (!foo.test(new foo.Derived2))
      return 'Fail #3';
    return 'OK';
  }
}
}, {Base:classes.Base, Abstract:classes.Abstract, Derived1:classes.Derived1, Derived2:classes.Derived2});
foo.initialize();
