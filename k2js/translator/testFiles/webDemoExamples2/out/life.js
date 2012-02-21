var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(width, height, init){
    this.$width = width;
    this.$height = height;
    this.$live = Kotlin.arrayFromFun(this.get_height(), (tmp$0_0 = this , function(i){
      {
        var tmp$0;
        return Kotlin.arrayFromFun(tmp$0_0.get_width(), (tmp$0 = tmp$0_0 , function(j){
          {
            return init(i, j);
          }
        }
        ));
      }
    }
    ));
  }
  , get_width:function(){
    return this.$width;
  }
  , get_height:function(){
    return this.$height;
  }
  , get_live:function(){
    return this.$live;
  }
  , liveCount:function(i, j){
    var tmp$0;
    if ((new Kotlin.NumberRange(0, this.get_height() - 1 - 0 + 1, false)).contains(i) && (new Kotlin.NumberRange(0, this.get_width() - 1 - 0 + 1, false)).contains(j) && this.get_live()[i][j])
      tmp$0 = 1;
    else 
      tmp$0 = 0;
    {
      return tmp$0;
    }
  }
  , liveNeighbors:function(i, j){
    {
      return this.liveCount(i - 1, j - 1) + this.liveCount(i - 1, j) + this.liveCount(i - 1, j + 1) + this.liveCount(i, j - 1) + this.liveCount(i, j + 1) + this.liveCount(i + 1, j - 1) + this.liveCount(i + 1, j) + this.liveCount(i + 1, j + 1);
    }
  }
  , get:function(i, j){
    {
      return this.get_live()[i][j];
    }
  }
  });
  return {Field:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, next:function(field){
  {
    return new Anonymous.Field(field.get_width(), field.get_height(), function(i, j){
      {
        var n = field.liveNeighbors(i, j);
        if (field.get(i, j))
          return (new Kotlin.NumberRange(2, 3 - 2 + 1, false)).contains(n);
        else 
          return n == 3;
      }
    }
    );
  }
}
, main:function(args){
  {
    Anonymous.printField('***', 3);
    Anonymous.printField('\n    __*__\n    _***_\n    __*__\n  ', 10);
    Anonymous.printField('\n    __*__\n    _*_*_\n    __*__\n  ', 3);
    Anonymous.printField('\n    __**__\n    __**__\n    __**__\n  ', 3);
    Anonymous.printField('\n    __**__\n    __**__\n      __**__\n      __**__\n  ', 6);
    Anonymous.printField('\n    ---------------\n    ---***---***---\n    ---------------\n    -*----*-*----*-\n    -*----*-*----*-\n    -*----*-*----*-\n    ---***---***---\n    ---------------\n    ---***---***---\n    -*----*-*----*-\n    -*----*-*----*-\n    -*----*-*----*-\n    ---------------\n    ---***---***---\n    ---------------\n  ', 10);
  }
}
, printField:function(s, steps){
  {
    var tmp$0;
    var field = Anonymous.makeField(s);
    {
      tmp$0 = (new Kotlin.NumberRange(1, steps - 1 + 1, false)).iterator();
      while (tmp$0.hasNext()) {
        var step = tmp$0.next();
        {
          var tmp$1;
          Kotlin.println('Step: ' + step);
          {
            tmp$1 = (new Kotlin.NumberRange(0, field.get_height() - 1 - 0 + 1, false)).iterator();
            while (tmp$1.hasNext()) {
              var i = tmp$1.next();
              {
                var tmp$2;
                {
                  tmp$2 = (new Kotlin.NumberRange(0, field.get_width() - 1 - 0 + 1, false)).iterator();
                  while (tmp$2.hasNext()) {
                    var j = tmp$2.next();
                    {
                      var tmp$3;
                      if (field.get(i, j))
                        tmp$3 = '*';
                      else 
                        tmp$3 = ' ';
                      Kotlin.print(tmp$3);
                    }
                  }
                }
                Kotlin.println('');
              }
            }
          }
          field = Anonymous.next(field);
        }
      }
    }
  }
}
, toList:function(receiver){
  {
    return Anonymous.to(receiver, new Kotlin.ArrayList);
  }
}
, get_size:function(receiver){
  var tmp$0;
  if (receiver != null)
    tmp$0 = receiver.length;
  else 
    tmp$0 = 0;
  {
    return tmp$0;
  }
}
, to:function(receiver, result){
  {
    var tmp$0;
    {
      tmp$0 = Kotlin.arrayIterator(receiver);
      while (tmp$0.hasNext()) {
        var elem = tmp$0.next();
        {
          result.add(elem);
        }
      }
    }
    return result;
  }
}
, makeField:function(s){
  {
    var tmp$2;
    var tmp$0;
    var lines = Kotlin.splitString(s, '\n');
    var w = Kotlin.sure(Kotlin.collectionsMax(Anonymous.toList(lines), Kotlin.comparator(function(o1, o2){
      {
        var l1 = Anonymous.get_size(o1);
        var l2 = Anonymous.get_size(o2);
        return l1 - l2;
      }
    }
    )));
    var data = Kotlin.arrayFromFun(lines.length, function(it_0){
      {
        return Kotlin.arrayFromFun(Anonymous.get_size(w), function(it){
          {
            return false;
          }
        }
        );
      }
    }
    );
    {
      tmp$0 = Kotlin.arrayIndices(data).iterator();
      while (tmp$0.hasNext()) {
        var i_0 = tmp$0.next();
        {
          var tmp$1;
          data[i_0] = Kotlin.arrayFromFun(Anonymous.get_size(w), function(it){
            {
              return false;
            }
          }
          );
          {
            tmp$1 = Kotlin.arrayIndices(data[i_0]).iterator();
            while (tmp$1.hasNext()) {
              var j_0 = tmp$1.next();
              {
                data[i_0][j_0] = false;
              }
            }
          }
        }
      }
    }
    {
      tmp$2 = Kotlin.arrayIndices(lines).iterator();
      while (tmp$2.hasNext()) {
        var line = tmp$2.next();
        {
          var tmp$3;
          {
            tmp$3 = Anonymous.get_indices(lines[line]).iterator();
            while (tmp$3.hasNext()) {
              var x = tmp$3.next();
              {
                var c = lines[line].charAt(x);
                data[line][x] = c == '*';
              }
            }
          }
        }
      }
    }
    return new Anonymous.Field(Anonymous.get_size(w), lines.length, function(i, j){
      {
        return data[i][j];
      }
    }
    );
  }
}
, get_indices:function(receiver){
  {
    return new Kotlin.NumberRange(0, Anonymous.get_size(Kotlin.sure(receiver)));
  }
}
, set:function(receiver, k, v){
  {
    receiver.put(k, v);
  }
}
, get_isEmpty:function(receiver){
  {
    return receiver.length == 0;
  }
}
}, {Field:classes.Field});
Anonymous.initialize();
