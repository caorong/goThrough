<%@ page import="java.util.Map" %>
<%--
Created by IntelliJ IDEA.
User: spance
Date: 14/6/8
Time: 1:42
To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
    <style>
        table td {
            font-size: 12px;
            font-family: Monaco, "Lucida Console", Consolas, monospace;
        }
    </style>
</head>
<body>
<h1>env</h1>
<table>
    <%
        for (Map.Entry<String, String> entry : System.getenv().entrySet())
            out.println(String.format("<tr><td>%s</td><td>%s</td></tr>", entry.getKey(), entry.getValue()));
    %>
</table>
<h1>properties</h1>
<table>
    <%
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet())
            out.println(String.format("<tr><td>%s</td><td>%s</td></tr>", entry.getKey(), entry.getValue()));
    %>
</table>
</body>
</html>
