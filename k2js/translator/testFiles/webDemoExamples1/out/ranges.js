var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    var tmp$0;
    var x = Kotlin.parseInt(args[0]);
    var y = 10;
    if ((new Kotlin.NumberRange(1, y - 1 - 1 + 1, false)).contains(x))
      Kotlin.println('OK');
    {
      tmp$0 = (new Kotlin.NumberRange(1, 5 - 1 + 1, false)).iterator();
      while (tmp$0.hasNext()) {
        var a = tmp$0.next();
        {
          Kotlin.print(' ' + a);
        }
      }
    }
    Kotlin.println();
    var array = new Kotlin.ArrayList;
    array.add('aaa');
    array.add('bbb');
    array.add('ccc');
    if (!(new Kotlin.NumberRange(0, array.size() - 0 + 1, false)).contains(x))
      Kotlin.println('Out: array has only ' + array.size() + ' elements. x = ' + x);
    if (array.contains('aaa'))
      Kotlin.println('Yes: array contains aaa');
    if (array.contains('ddd'))
      Kotlin.println('Yes: array contains ddd');
    else 
      Kotlin.println("No: array doesn't contains ddd");
  }
}
}, {});
Anonymous.initialize();
