<!DOCTYPE html>
<html>
<head>
    <title>Whydah OpenID Connect Logon Consent!</title>
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
   margin-top: 20px;
   transform: translate(-50%, -50%);
}

/* For mobile phones: */

 .cover {
  	object-fit: scale-down;
  	max-width: 98%;
 }
 

@media only screen and (min-width: 600px) {
  /* For tablets: */
  .modal {
         max-width: 760px;
  }

  .cover {
  		object-fit: scale-down;
  		height: 100px;
  }
}

@media only screen and (min-width: 768px) {
  /* For desktop: */
  .modal {
         min-width: 700px;
         margin-top: 50px;
  }

  .cover {
  		object-fit: scale-down;
  		height: 150px;
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

img {
  display: block;
  margin-left: auto;
  margin-right: auto;
}


</style>
</head>

<body style="background-color: #eef;>
<div id="page-content">
    <div id="logo">
    	<figure>
        	<img src="${logoURL!}" alt="Whydah OpenID Connect Concent Company Logo" class="cover" />
        </figure>
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
    <input type="hidden" id="response_mode" name="response_mode" value="${response_mode}">
    <input type="hidden" id="state" name="state" value="${state}">
    <input type="hidden" id="redirect_uri" name="redirect_uri" value="${redirect_uri}">
    <input type="hidden" id="usertoken_id" name="usertoken_id" value="${usertoken_id}">
	<input type="hidden" id="nonce" name="nonce" value="${nonce}">
	<#if referer_channel??>
	<input type="hidden" id="referer_channel" name="referer_channel" value="${referer_channel}">
	</#if>
	<#if code_challenge??>
	<input type="hidden" id="code_challenge" name="code_challenge" value="${code_challenge}">
	</#if>
	<#if code_challenge_method??>
	<input type="hidden" id="code_challenge_method" name="code_challenge_method" value="${code_challenge_method}">
	</#if>
    <input type="radio" name="accepted" value="yes" checked="checked"> Accept<br>
    <input type="radio" name="accepted" value="no"> Decline<br>
    <input type="submit" id="submit" name="submit" value="Next" style="background-color: #2D7BB2;">
</form>
</div>
</div>
</div>
</body>
</html>