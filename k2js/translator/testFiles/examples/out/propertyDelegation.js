var classes = function(){
  var tmp$0 = Kotlin.Trait.create({});
  var tmp$1 = Kotlin.Class.create({initialize:function(){
    this.$plain = 239;
    this.$readwrite = 0;
  }
  , get_plain:function(){
    return this.$plain;
  }
  , get_read:function(){
    {
      return 239;
    }
  }
  , get_readwrite:function(){
    {
      return this.$readwrite + 1;
    }
  }
  , set_readwrite:function(n){
    {
      this.$readwrite = n;
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
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, code:function(s){
  {
    if (s.get_plain() != 239)
      return 1;
    if (s.get_read() != 239)
      return 2;
    s.set_readwrite(238);
    if (s.get_readwrite() != 239)
      return 3;
    return 0;
  }
}
, test:function(s){
  {
    return Anonymous.code(s) == 0;
  }
}
, box:function(){
  {
    if (!Anonymous.test(new Anonymous.Base))
      return 'Fail #1';
    if (!Anonymous.test(new Anonymous.Derived1))
      return 'Fail #2';
    if (!Anonymous.test(new Anonymous.Derived2))
      return 'Fail #3';
    return 'OK';
  }
}
}, {Base:classes.Base, Abstract:classes.Abstract, Derived1:classes.Derived1, Derived2:classes.Derived2});
Anonymous.initialize();
