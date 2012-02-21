var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $b = 0;
}
, get_b:function(){
  return $b;
}
, set_b:function(tmp$0){
  $b = tmp$0;
}
, loop:function(times){
  {
    while (times > 0) {
      var u = function(value){
        {
          foo.set_b(foo.get_b() + 1);
        }
      }
      ;
      u(times--);
    }
  }
}
, box:function(){
  {
    foo.loop(5);
    return foo.get_b() == 5;
  }
}
}, {});
foo.initialize();
