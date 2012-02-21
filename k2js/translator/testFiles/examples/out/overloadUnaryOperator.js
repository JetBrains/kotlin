var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$contents = new Kotlin.ArrayList;
  }
  , get_contents:function(){
    return this.$contents;
  }
  , add:function(item){
    {
      this.get_contents().add(item);
    }
  }
  , minus:function(){
    {
      var tmp$0;
      var result = new Anonymous.ArrayWrapper;
      result.get_contents().addAll(this.get_contents());
      var i = this.get_contents().size();
      {
        tmp$0 = this.get_contents().iterator();
        while (tmp$0.hasNext()) {
          var a = tmp$0.next();
          {
            result.get_contents().set(--i, a);
          }
        }
      }
      return result;
    }
  }
  , get:function(index){
    {
      return this.get_contents().get(index);
    }
  }
  });
  return {ArrayWrapper:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var v1 = new Anonymous.ArrayWrapper;
    v1.add('foo');
    v1.add('bar');
    var v2 = v1.minus();
    if (v2.get(0) == 'bar' && v2.get(1) == 'foo')
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
}, {ArrayWrapper:classes.ArrayWrapper});
Anonymous.initialize();
