<!doctype html>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<head>
    <meta charset="utf-8">
    <title>AB Testing Demo</title>
    <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { 
            height: 100%; 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
<c:if test="${not empty groups.bgcolortstPayload and not empty groups.bgcolortstPayload.bgcolor}">
            background-color: ${groups.bgcolortstPayload.bgcolor}; 
            color: #ffffff;
</c:if>
<c:if test="${empty groups.bgcolortstPayload or empty groups.bgcolortstPayload.bgcolor}">
            background-color: #ffffff;
            color: #333333;
</c:if>
        }
        .container {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100%;
            text-align: center;
        }
        h1 { font-size: 3rem; margin-bottom: 1rem; }
        .bucket { font-size: 1.5rem; opacity: 0.8; }
    </style>
</head>
<body>
    <div class="container">
<c:choose>
            <c:when test="${not empty greeting}">
        <h1>${greeting}, this is an AB Testing Demo!</h1>
            </c:when>
            <c:otherwise>
        <h1>Welcome, this is an AB Testing Demo!</h1>
            </c:otherwise>
        </c:choose>
    </div>
    <script>
        setTimeout(function() { location.reload(); }, 5000);
    </script>
</body>
</html>
