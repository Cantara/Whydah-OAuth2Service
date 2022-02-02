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

<body style="background-color: #eef;">
<div id="page-content">
    <div id="logo">
    	<figure>
        	<img src="${logoURL!}" alt="Whydah OpenID Connect Concent Company Logo" class="cover" />
        </figure>
    </div>
<div class="modal">
<div id="page-content" >
<h1>Logout confirmation</h1>
<p>
  Would you like to log out ${username}?
</p>

<form action="logout/confirm" method="post" >
    <input type="hidden" id="redirect_uri" name="redirect_uri" value="${redirect_uri}">
    <input type="hidden" id="usertoken_id" name="usertoken_id" value="${usertoken_id}">
    <input type="hidden" id="state" name="state" value="${state}">
    <input type="radio" name="accepted" value="yes" checked="checked"> Yes<br>
    <input type="radio" name="accepted" value="no"> No<br>
    <input type="submit" id="submit" name="submit" value="Confirm" style="background-color: #2D7BB2;">
</form>
</div>
</div>
</div>
</body>
</html>