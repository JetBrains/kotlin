var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , div:function(other){
    {
      return 'hooray';
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
    return (new foo.A).div(new foo.A) == 'hooray';
  }
}
}, {A:classes.A});
foo.initialize();
