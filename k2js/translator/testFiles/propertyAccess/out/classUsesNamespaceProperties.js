var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    {
      foo.set_a(foo.get_a() + 1);
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a = 0;
}
, get_a:function(){
  return $a;
}
, set_a:function(tmp$0){
  $a = tmp$0;
}
, box:function(){
  {
    var c = new foo.A;
    c = new foo.A;
    c = new foo.A;
    return foo.get_a() == 3;
  }
}
}, {A:classes.A});
foo.initialize();
