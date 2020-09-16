package net.whydah.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import net.whydah.commands.config.ConstantValue;
import net.whydah.service.oauth2proxyserver.RSAKeyFactory;



public class JwtUtils {
	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

	public static String generateJwtToken(String jti, String subject, String issuer, String audience, Map<String, Object> claims, Date expiration) {	
		return Jwts.builder()
				.setSubject(subject)
				.setId(jti)
				.setIssuer(issuer)
				.setAudience(audience)
				.setClaims(claims)
				.setIssuedAt(new Date())
				.setExpiration(expiration)
				.signWith(SignatureAlgorithm.HS256, ConstantValue.KEYSECRET)
				.compact();
	}
	
	public static String generateJwtToken(Map<String, Object> claims, Date expiration) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date())
				.setExpiration(expiration)
				.signWith(SignatureAlgorithm.HS256, ConstantValue.KEYSECRET)
				.compact();
	}
	
	public static String generateJwtToken(Map<String, Object> claims, Date expiration, PrivateKey privateKey) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date())
				.setExpiration(expiration)
				.setHeaderParam("typ", "JWT")
				.setHeaderParam("kid", RSAKeyFactory.getKid())
				.signWith(SignatureAlgorithm.RS256, privateKey)
				.compact();
	} 
	
	public static Claims getClaims(String token, PublicKey publicKey) {
		return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token).getBody();
	}

	public static Claims getClaims(String token) {
		return Jwts.parser().setSigningKey(ConstantValue.KEYSECRET).parseClaimsJws(token).getBody();
	}

	public static boolean validateJwtToken(String authToken, PublicKey publicKey) {
		try {
			Jwts.parser().setSigningKey(publicKey).parseClaimsJws(authToken);
			return true;
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}
}
