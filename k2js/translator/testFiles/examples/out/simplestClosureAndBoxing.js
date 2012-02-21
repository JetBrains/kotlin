var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    if (Anonymous.int_invoker(function(){
      {
        return 7;
      }
    }
    ) == 7)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
, int_invoker:function(gen){
  {
    return gen();
  }
}
}, {});
Anonymous.initialize();
