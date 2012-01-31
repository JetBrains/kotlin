foo = {};
(function(foo){
  foo.Test = Kotlin.Trait.create({addFoo:function(s){
    return s + 'FOO';
  }
  });
  foo.ExtendedTest = Kotlin.Trait.create(foo.Test, {hooray:function(){
    return 'hooray';
  }
  });
}
(foo));
