foo = {};
(function(foo){
  foo.y = 3;
  foo.f = function(a){
    var x = 42;
    var y = 50;
    return y;
  }
  ;
  foo.box = function(){
    return foo.f(foo.y);
  }
  ;
}
(foo));
