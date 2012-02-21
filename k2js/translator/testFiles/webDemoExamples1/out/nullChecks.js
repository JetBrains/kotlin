var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, myParseInt:function(str){
  {
    try {
      return Kotlin.parseInt(str);
    }
     catch (e) {
      Kotlin.println("One of argument isn't Int");
    }
    return null;
  }
}
, main:function(args){
  {
    if (args.length < 2) {
      Kotlin.print('No number supplied');
    }
     else {
      var x = Anonymous.myParseInt(args[0]);
      var y = Anonymous.myParseInt(args[1]);
      if (x != null && y != null) {
        Kotlin.print(x * y);
      }
    }
  }
}
}, {});
Anonymous.initialize();
