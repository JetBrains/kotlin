var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var cl = 39;
    if (Anonymous.sum(200, function(){
      {
        var ff = function(){
          {
            return cl;
          }
        }
        ;
        return ff();
      }
    }
    ) == 239)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'FAIL';
    return tmp$0;
  }
}
, sum:function(arg, f){
  {
    return arg + f();
  }
}
}, {});
Anonymous.initialize();
