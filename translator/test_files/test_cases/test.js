foo = {};
(function(foo){
  foo.box = function(){
    var a = 2;
    var b = 3;
    var c = 4;
    return a < c;
  }
  ;
}
(foo));
