<!DOCTYPE html>
<html>
<head>
    <title>Whydah OpenID Connect Logon Concent!</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
<style>
.modal {
   border: 1px solid black;
   background-color: white;
   padding: 10px;
   border-radius: 10px;
   position: fixed;   
   top: 50%;
   left: 50%;
   transform: translate(-50%, -50%);
}


.center {
  display: block;
  margin-left: auto;
  margin-right: auto;
  width: 90%;
}

@media ( max-width :320px) {
    .modal {
         min-width: 200px;
         min-height: 120px;
    }
}

input[type=submit] {
  background-color: #eef;
  color: white;
  padding: 12px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  float: right;
}


@media ( min-width :321px) {
    .modal {
         background-color: white;
         min-width: 300px;
         min-height: 220px;
    }
}

@media ( min-width :640px) {
    .modal {
         background-color: white;
         min-width: 480px;
         min-height: 320px;
    }
}

@media ( min-width :1200px) {
    .modal {
         background-color: white;
         min-width: 700px;
         min-height: 400px;
    }
}
</style>
</head>

<body style="background-color: #eef;>
<div id="page-content">
    <div id="logo">
        <img src="${logoURL!}" alt="Whydah OpenID Connect Concent Compeny Logo" height="80" class="center"/>
    </div>
<div class="modal">
<div id="page-content" >
<h1>Welcome ${user.name}!</h1>

An application named <b>${client_name}</b> would like to access the following values from your userprofile:
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
    <input type="hidden" id="customer_ref" name="user_id" value="${customer_ref}">
    <input type="hidden" id="response_type" name="response_type" value="${response_type}">
    <input type="hidden" id="state" name="state" value="${state}">
    <input type="hidden" id="redirect_uri" name="redirect_uri" value="${redirect_uri}">
    <input type="hidden" id="usertoken_id" name="usertoken_id" value="${usertoken_id}">
	<input type="hidden" id="nonce" name="nonce" value="${nonce}">
    <input type="radio" name="accepted" value="yes" checked="checked"> Accept<br>
    <input type="radio" name="accepted" value="no"> Decline<br>
    <input type="submit" id="submit" name="submit" value="Next" style="background-color: #4CAF50;">
</form>
</div>
</div>
</div>
</body>
</html>