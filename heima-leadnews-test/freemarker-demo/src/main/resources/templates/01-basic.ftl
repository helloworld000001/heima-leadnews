<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Hello World!</title>
</head>
<body>
<b>普通文本 String 展示：</b><br><br>
<#-- 表示的是name不等于空值，也就是空值判断，否则当为null时会报错 -->
<#-- 如果name为空的时候默认显示空字符串 -->
Hello ${name!''} <br>
<hr>
<b>对象Student中的数据展示：</b><br/>
姓名：${stu.name}<br/>
年龄：${stu.age}
<hr>
</body>
</html>