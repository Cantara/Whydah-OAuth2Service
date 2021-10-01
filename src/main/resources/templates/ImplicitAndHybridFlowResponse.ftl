<html>
   <head><title>Submit This Form</title></head>
   <body onload="javascript:document.forms[0].submit()">
    <form method="post" action="${redirect_uri}">
      <#if code??>
      	<input type="hidden" name="code" value="${code}"/>
      </#if>
      <#if id_token??>
      	<input type="hidden" name="id_token" value="${id_token}"/>
      </#if>
      <#if access_token??>
      	<input type="hidden" name="access_token" value="${access_token}"/>
      </#if>
      <#if token_type??>
      	<input type="hidden" name="token_type" value="${token_type}"/>
      </#if>
      <#if expires_in??>
      	<input type="hidden" name="expires_in" value="${expires_in}"/>
      </#if>
       <#if state??>
      	<input type="hidden" name="state" value="${state}"/>
      </#if>
       <#if nonce??>
      	<input type="hidden" name="nonce" value="${nonce}"/>
      </#if>
      <#if error??>
      	<input type="hidden" name="error" value="${error}"/>
      </#if>
      
    </form>
   </body>
  </html>