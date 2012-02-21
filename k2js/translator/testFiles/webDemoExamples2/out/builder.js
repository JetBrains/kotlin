var classes = function(){
  var tmp$0 = Kotlin.Trait.create({render:function(builder, indent){
  }
  , toString:function(){
    {
      var builder = new Kotlin.StringBuilder;
      this.render(builder, '');
      return builder.toString();
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(name_0){
    this.$name = name_0;
    this.$children = new Kotlin.ArrayList;
    this.$attributes = new Kotlin.HashMap;
  }
  , get_name:function(){
    return this.$name;
  }
  , get_children:function(){
    return this.$children;
  }
  , get_attributes:function(){
    return this.$attributes;
  }
  , initTag:function(tag, init){
    {
      init.call(tag);
      this.get_children().add(tag);
      return tag;
    }
  }
  , render:function(builder, indent){
    {
      var tmp$0;
      builder.append(indent + '<' + this.get_name() + this.renderAttributes() + '>' + '\n');
      {
        tmp$0 = this.get_children().iterator();
        while (tmp$0.hasNext()) {
          var c = tmp$0.next();
          {
            c.render(builder, indent + '  ');
          }
        }
      }
      builder.append(indent + '<\/' + this.get_name() + '>' + '\n');
    }
  }
  , renderAttributes:function(){
    {
      var tmp$0;
      var builder = new Kotlin.StringBuilder;
      {
        tmp$0 = this.get_attributes().keySet().iterator();
        while (tmp$0.hasNext()) {
          var a = tmp$0.next();
          {
            builder.append(' ' + a + '=' + '"' + this.get_attributes().get(a) + '"');
          }
        }
      }
      return builder.toString();
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, {initialize:function(name_0){
    this.super_init(name_0);
  }
  , plus:function(receiver){
    {
      this.get_children().add(new Anonymous.TextElement(receiver));
    }
  }
  });
  var tmp$3 = Kotlin.Class.create(tmp$2, {initialize:function(name_0){
    this.super_init(name_0);
  }
  , b:function(init){
    {
      return this.initTag(new Anonymous.B, init);
    }
  }
  , p:function(init){
    {
      return this.initTag(new Anonymous.P, init);
    }
  }
  , h1:function(init){
    {
      return this.initTag(new Anonymous.H1, init);
    }
  }
  , ul:function(init){
    {
      return this.initTag(new Anonymous.UL, init);
    }
  }
  , a_0:function(href, init){
    {
      var a = this.initTag(new Anonymous.A, init);
      a.set_href(href);
    }
  }
  });
  var tmp$4 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('a');
  }
  , get_href:function(){
    {
      return this.get_attributes().get('href');
    }
  }
  , set_href:function(value){
    {
      Anonymous.set(this.get_attributes(), 'href', value);
    }
  }
  });
  var tmp$5 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('h1');
  }
  });
  var tmp$6 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('p');
  }
  });
  var tmp$7 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('li');
  }
  });
  var tmp$8 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('b');
  }
  });
  var tmp$9 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('ul');
  }
  , li:function(init){
    {
      return this.initTag(new Anonymous.LI, init);
    }
  }
  });
  var tmp$10 = Kotlin.Class.create(tmp$3, {initialize:function(){
    this.super_init('body');
  }
  });
  var tmp$11 = Kotlin.Class.create(tmp$2, {initialize:function(){
    this.super_init('title');
  }
  });
  var tmp$12 = Kotlin.Class.create(tmp$2, {initialize:function(){
    this.super_init('head');
  }
  , title:function(init){
    {
      return this.initTag(new Anonymous.Title, init);
    }
  }
  });
  var tmp$13 = Kotlin.Class.create(tmp$2, {initialize:function(){
    this.super_init('html');
  }
  , head:function(init){
    {
      return this.initTag(new Anonymous.Head, init);
    }
  }
  , body:function(init){
    {
      return this.initTag(new Anonymous.Body, init);
    }
  }
  });
  var tmp$14 = Kotlin.Class.create(tmp$0, {initialize:function(text){
    this.$text = text;
  }
  , get_text:function(){
    return this.$text;
  }
  , render:function(builder, indent){
    {
      builder.append(indent + this.get_text() + '\n');
    }
  }
  });
  return {Title:tmp$11, Body:tmp$10, HTML:tmp$13, Head:tmp$12, TextElement:tmp$14, UL:tmp$9, H1:tmp$5, P:tmp$6, LI:tmp$7, B:tmp$8, Tag:tmp$1, TagWithText:tmp$2, BodyTag:tmp$3, A:tmp$4, Element_0:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    var result = Anonymous.html_0(function(){
      {
        this.head(function(){
          {
            this.title(function(){
              {
                this.plus('XML encoding with Kotlin');
              }
            }
            );
          }
        }
        );
        this.body(function(){
          {
            this.h1(function(){
              {
                this.plus('XML encoding with Kotlin');
              }
            }
            );
            this.p(function(){
              {
                this.plus('this format can be used as an alternative markup to XML');
              }
            }
            );
            this.a_0('http://jetbrains.com/kotlin', function(){
              {
                this.plus('Kotlin');
              }
            }
            );
            this.p(function(){
              {
                this.plus('This is some');
                this.b(function(){
                  {
                    this.plus('mixed');
                  }
                }
                );
                this.plus('text. For more see the');
                this.a_0('http://jetbrains.com/kotlin', function(){
                  {
                    this.plus('Kotlin');
                  }
                }
                );
                this.plus('project');
              }
            }
            );
            this.p(function(){
              {
                this.plus('some text');
              }
            }
            );
            this.p(function(){
              {
                this.plus('Command line arguments were:');
                this.ul(function(){
                  {
                    var tmp$0;
                    {
                      tmp$0 = Kotlin.arrayIterator(args);
                      while (tmp$0.hasNext()) {
                        var arg = tmp$0.next();
                        {
                          this.li(function(){
                            {
                              this.plus(arg);
                            }
                          }
                          );
                        }
                      }
                    }
                  }
                }
                );
              }
            }
            );
          }
        }
        );
      }
    }
    );
    Kotlin.println(result);
  }
}
, html_0:function(init){
  {
    var html = new Anonymous.HTML;
    init.call(html);
    return html;
  }
}
, set:function(receiver, key, value){
  {
    return receiver.put(key, value);
  }
}
}, {Element_0:classes.Element_0, TextElement:classes.TextElement, Tag:classes.Tag, TagWithText:classes.TagWithText, HTML:classes.HTML, Head:classes.Head, Title:classes.Title, BodyTag:classes.BodyTag, Body:classes.Body, UL:classes.UL, B:classes.B, LI:classes.LI, P:classes.P, H1:classes.H1, A:classes.A});
Anonymous.initialize();
