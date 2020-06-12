package net.whydah.util;

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
	

	public static Claims getClaims(String token) {
		return Jwts.parser().setSigningKey(ConstantValue.KEYSECRET).parseClaimsJws(token).getBody();
	}

	public static boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser().setSigningKey(ConstantValue.KEYSECRET).parseClaimsJws(authToken);
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
