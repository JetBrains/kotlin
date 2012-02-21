var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , plus:function(){
    {
      return 'hooray';
    }
  }
  , minus:function(){
    {
      return 'not really';
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new foo.A;
    return c.plus() + c.minus() == 'hooraynot really';
  }
}
}, {A:classes.A});
foo.initialize();
