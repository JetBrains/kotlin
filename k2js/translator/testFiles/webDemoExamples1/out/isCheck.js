var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    Kotlin.println(Anonymous.getStringLength('aaa'));
    Kotlin.println(Anonymous.getStringLength(1));
  }
}
, getStringLength:function(obj){
  {
    if (typeof obj == 'string')
      return obj.length;
    return null;
  }
}
}, {});
Anonymous.initialize();
