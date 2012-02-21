var classes = function(){
  var tmp$0 = Kotlin.Trait.create({addFoo:function(s){
    {
      return s + 'FOO';
    }
  }
  , addBar:function(s){
    {
      return s + 'BAR';
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.$string = 'TEST';
  }
  , get_string:function(){
    return this.$string;
  }
  , value:function(){
    {
      return this.addBar(this.addFoo(this.get_string()));
    }
  }
  });
  return {A:tmp$1, Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).value() == 'TESTFOOBAR';
  }
}
}, {Test:classes.Test, A:classes.A});
foo.initialize();
