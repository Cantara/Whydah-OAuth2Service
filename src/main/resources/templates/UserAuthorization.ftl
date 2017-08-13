<html>
<head>
    <title>Welcome!</title>
</head>

<body>
<h1>Welcome ${user.name}!</h1>

${client_id} would like read these values from your profile:
<p>Scopes:<br />
<#list scopeList as scope>
${scope}<br />
</#list>
</p>
<form action="authorize/acceptance" method="post" >
    <input type="hidden" id="client_id" name="client_id" value="${client_id}">
    <input type="hidden" id="scope" name="scopes" value="${scope}">
    <input type="hidden" id="user_id" name="user_id" value="${user.id}">
    <input type="hidden" id="code" name="code" value="${code}">
    <input type="hidden" id="state" name="state" value="${state}">
    <input type="hidden" id="redirect_url" name="redirect_url" value="${redirect_url}">
    <input type="hidden" id="usertoken_id" name="usertoken_id" value="${usertoken_id}">

    <input type="radio" name="accepted" value="yes" checked="checked"> Accept<br>
    <input type="radio" name="accepted" value="no"> Decline<br>
    <input type="submit" id="submit" name="submit" value="Next">
</form>
</body>
</html>