var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  return {MyClass:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    Anonymous.cases('Hello');
    Anonymous.cases(1);
    Anonymous.cases(new Anonymous.MyClass);
    Anonymous.cases('hello');
  }
}
, cases:function(obj){
  {
    var tmp$1;
    var tmp$0;
    for (tmp$0 = 0; tmp$0 < 4; ++tmp$0) {
      if (tmp$0 == 0)
        if (obj == 1) {
          tmp$1 = Kotlin.println('One');
          break;
        }
      if (tmp$0 == 1)
        if (obj == 'Hello') {
          tmp$1 = Kotlin.println('Greeting');
          break;
        }
      if (tmp$0 == 2)
        if (!(typeof obj == 'string')) {
          tmp$1 = Kotlin.println('Not a string');
          break;
        }
      if (tmp$0 == 3)
        tmp$1 = Kotlin.println('Unknown');
    }
    tmp$1;
  }
}
}, {MyClass:classes.MyClass});
Anonymous.initialize();
