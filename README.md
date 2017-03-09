# android-ueueo-log
Android log打印，支持打印xml，json和对象，并且格式化输出

```
compile 'com.ueueo:log:2.0'
```

```

UELog.append("UELog日志特性展示");
UELog.append("输出Json格式字符串");
UELog.appendJson("{\"id\":221,\"name\":\"my name is ueueo\",\"desc\":\"this is description!\"}");
UELog.append("输出Xml格式字符串");
UELog.appendXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><title>this is a title</title><body>这个是网页</body></html>");
UELog.append("输出对象");
UELog.appendObject(user);
UELog.i("输出日志");
                
```

