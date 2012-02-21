var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a1 = Kotlin.nullArray(3);
  $a2 = Kotlin.nullArray(2);
}
, get_a1:function(){
  return $a1;
}
, get_a2:function(){
  return $a2;
}
, box:function(){
  {
    return foo.get_a1().length == 3 && foo.get_a2().length == 2;
  }
}
}, {A:classes.A});
foo.initialize();
