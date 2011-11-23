foo = {};
(function(foo){
  foo.Test = Trait.create({addFoo:function(s){
    return s + 'FOO';
  }
  });
  foo.ExtendedTest = Trait.create(foo.Test, {hooray:function(){
    return 'hooray';
  }
  });
}
(foo));
