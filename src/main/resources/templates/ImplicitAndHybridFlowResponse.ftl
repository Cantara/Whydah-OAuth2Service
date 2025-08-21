<!DOCTYPE html>
<html>
   <head><title>Whydah OAuth2 Implicit and Hybrid Flow Submission</title></head>
   <body onload="javascript:document.forms[0].submit()">
    <form method="post" action="${redirect_uri}">
      <#if code??>
      	<input type="hidden" name="code" value="${code?html}"/>
      </#if>
      <#if id_token??>
      	<input type="hidden" name="id_token" value="${id_token?html}"/>
      </#if>
      <#if access_token??>
      	<input type="hidden" name="access_token" value="${access_token?html}"/>
      </#if>
      <#if token_type??>
      	<input type="hidden" name="token_type" value="${token_type?html}"/>
      </#if>
      <#if expires_in??>
      	<input type="hidden" name="expires_in" value="${expires_in?html}"/>
      </#if>
      <#if state??>
      	<input type="hidden" name="state" value="${state?html}"/>
      </#if>
      <#if nonce??>
      	<input type="hidden" name="nonce" value="${nonce?html}"/>
      </#if>
      <#if referer_channel??>
      	<input type="hidden" name="referer_channel" value="${referer_channel?html}"/>
      </#if>
      <#if code_challenge??>
      	<input type="hidden" name="code_challenge" value="${code_challenge?html}"/>
      </#if>
      <#if code_challenge_method??>
      	<input type="hidden" name="code_challenge_method" value="${code_challenge_method?html}"/>
      </#if>
      <#if error??>
      	<input type="hidden" name="error" value="${error?html}"/>
      </#if>
      <#if error_description??>
      	<input type="hidden" name="error_description" value="${error_description?html}"/>
      </#if>
    </form>
   </body>
</html>