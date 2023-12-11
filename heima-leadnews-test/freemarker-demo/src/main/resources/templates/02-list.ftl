<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/html">
<head>
    <meta charset="utf-8">
    <title>Hello World!</title>
</head>
<body>

<#-- list 数据的展示 -->
<b>展示list中的stu数据:</b>
<br>
<br>
<table>
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
    </tr>
    <#-- stus??用于判断stu是否为null -->
    <#if stus??>
    <#-- 取出stus这个list集合到，每取出一个存为stu-->
        <#list stus as stu>
        <#-- 在freemarker中，==和=的效果是一样的 -->
            <#if stu.name='小红'>
                <tr style="color: orange">
                    <#-- 当前索引（从0开始）序号是索引+1-->
                    <td>${stu_index + 1}</td>
                    <td>${stu.name}</td>
                    <td>${stu.age}</td>
                    <td>${stu.money}</td>
                </tr>
            <#else >
                <tr>
                    <#-- 当前索引（从0开始）序号是索引+1-->
                    <td>${stu_index + 1}</td>
                    <td>${stu.name}</td>
                    <td>${stu.age}</td>
                    <td>${stu.money}</td>
                </tr>
            </#if>
        </#list>
    </#if>

    stus集合的大小：${stus?size}

</table>
<hr>

<#-- Map 数据的展示 -->
<b>map数据的展示：</b>
<br/><br/>
<a href="###">方式一：通过map['keyname'].property</a><br/>
输出stu1的学生信息：<br/>
姓名：${stuMap['stu1'].name}<br/>
年龄：${stuMap['stu1'].age}<br/>
<br/>
<a href="###">方式二：通过map.keyname.property</a><br/>
输出stu2的学生信息：<br/>
姓名：${stuMap.stu2.name}<br/>
年龄：${stuMap.stu2.age}<br/>

<br/>
<a href="###">遍历map中两个学生信息：</a><br/>
<table>
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
    </tr>
    <#-- 获取map中所有的keys,分别取出作为key -->
    <#list stuMap?keys as key>
        <tr>
            <td>${key_index + 1}</td>
            <td>${stuMap[key].name}</td>
            <td>${stuMap[key].age}</td>
            <td>${stuMap[key].money}</td>
        </tr>
    </#list>
</table>
<hr>

<#-- today是存入model的数据，datetime表示显示date+time格式 -->
当前的日期为：${today?datetime} <br>

当前的日期为：${today?string("yyyy年MM月")} <br>

-------------------------------------------<br>

<#-- 127,891,214,241,231 默认显示是每三位用逗号隔开 -->
${point} <br>

<#-- 127891214241231 取消使用逗号隔开 -->
${point?c}

</body>
</html>