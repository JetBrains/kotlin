var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(name_0){
    this.$name = name_0;
  }
  , get_name:function(){
    return this.$name;
  }
  , greet:function(){
    {
      Kotlin.println('Hello, ' + this.get_name() + '!');
    }
  }
  });
  return {Greeter:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    (new Anonymous.Greeter(args[0])).greet();
  }
}
}, {Greeter:classes.Greeter});
Anonymous.initialize();
