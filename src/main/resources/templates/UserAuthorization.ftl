<html>
<head>
    <title>Single-Signon to application request!</title>
      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
      <link rel="stylesheet" href="css/whydah.css" type="text/css"/>
      <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
      <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
</head>

<body>
<div style="width:480px;margin:auto;">
<div id="page-content">
<h1>Welcome ${user.name}!</h1>

An application named ${client_name} would like to access the following values from your userprofile:
<p>Scopes:<br />
<ul style="list-style: none;">
<#list scopeList as scope>
  <li>${scope}</li>
</#list>
</ul>
</p>
<form action="authorize/acceptance" method="post" >
    <input type="hidden" id="client_id" name="client_id" value="${client_id}">
    <input type="hidden" id="scope" name="scope" value="${scope}">
    <input type="hidden" id="user_id" name="user_id" value="${user.id}">
    <input type="hidden" id="response_type" name="response_type" value="${response_type}">
    <input type="hidden" id="state" name="state" value="${state}">
    <input type="hidden" id="redirect_uri" name="redirect_uri" value="${redirect_uri}">
    <input type="hidden" id="usertoken_id" name="usertoken_id" value="${usertoken_id}">

    <input type="radio" name="accepted" value="yes" checked="checked"> Accept<br>
    <input type="radio" name="accepted" value="no"> Decline<br>
    <input type="submit" id="submit" name="submit" value="Next">
</form>
</div>
</div>
</body>
</html>