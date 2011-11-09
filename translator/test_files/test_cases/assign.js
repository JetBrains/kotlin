foo = {};
(function(foo){
  foo.f = function(){
    var x = 1;
    x = x + 1;
    return x;
  }
  ;
}
(foo));
