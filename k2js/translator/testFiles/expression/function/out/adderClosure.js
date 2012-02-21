var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var sum = 0;
    var adder = function(a){
      {
        sum += a;
      }
    }
    ;
    adder(3);
    adder(2);
    return sum == 5;
  }
}
}, {});
foo.initialize();
