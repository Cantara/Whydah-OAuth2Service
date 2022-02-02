package net.whydah.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import net.whydah.commands.config.ConfiguredValue;
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
				.setIssuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(2))))
				//.setExpiration(Date.from(expiration.toInstant().plus(Duration.ofMinutes(2))))
				.setExpiration(expiration)
				.signWith(SignatureAlgorithm.HS256, ConfiguredValue.KEYSECRET)
				.compact();
	}
	
	public static String generateJwtToken(Map<String, Object> claims, Date expiration) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(2))))
				//.setExpiration(Date.from(expiration.toInstant().plus(Duration.ofMinutes(2))))
				.setExpiration(expiration)
				.signWith(SignatureAlgorithm.HS256, ConfiguredValue.KEYSECRET)
				.compact();
	}
	
	public static String generateJwtToken(Map<String, Object> claims, Date expiration, PrivateKey privateKey) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(2))))
				//.setExpiration(Date.from(expiration.toInstant().plus(Duration.ofMinutes(2))))
				.setExpiration(expiration)
				.setHeaderParam("typ", "JWT")
				.setHeaderParam("kid", RSAKeyFactory.getKid())
				.signWith(SignatureAlgorithm.RS256, privateKey)
				.compact();
		
		
		
	}
	
	public static Claims getClaims(HttpServletRequest request, PublicKey publicKey) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			String jwt = headerAuth.substring(7, headerAuth.length());
			if(validateJwtToken(jwt, publicKey)) {
				return getClaims(jwt, publicKey);
			}
		}
		return null;
	}
	
	public static Claims getClaims(String token, PublicKey publicKey) {
		return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token).getBody();
	}

	public static Claims getClaims(String token) {
		return Jwts.parser().setSigningKey(ConfiguredValue.KEYSECRET).parseClaimsJws(token).getBody();
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
