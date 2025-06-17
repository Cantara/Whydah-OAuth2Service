package net.whydah.util;

import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import net.whydah.commands.config.ConstantValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;



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
				.signWith(SignatureAlgorithm.HS256, ConstantValues.KEYSECRET)
				.compact();
	}
	
	public static String generateJwtToken(Map<String, Object> claims, Date expiration) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(2))))
				//.setExpiration(Date.from(expiration.toInstant().plus(Duration.ofMinutes(2))))
				.setExpiration(expiration)
				.signWith(SignatureAlgorithm.HS256, ConstantValues.KEYSECRET)
				.compact();
	}
	
	public static String generateRSAJwtToken(Map<String, Object> claims, Date expiration) {	
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(Date.from(Instant.now().minus(Duration.ofMinutes(2))))
				//.setExpiration(Date.from(expiration.toInstant().plus(Duration.ofMinutes(2))))
				.setExpiration(expiration)
				.setHeaderParam("typ", "JWT")
				.setHeaderParam("kid", RSAKeyFactory.getKId())
				.signWith(SignatureAlgorithm.RS256, RSAKeyFactory.getKeyPair().getPrivate())
				.compact();
		
		
		
	}
	
	static SigningKeyResolverAdapter keyResolver = new SigningKeyResolverAdapter() {
		@Override
		public Key resolveSigningKey(JwsHeader header, Claims claims) {
			final String keyId = header.getKeyId();
			return RSAKeyFactory.findPublicKey(keyId);	
		}
	};
	
	public static Claims parseRSAJwtToken(String token) {
		try {
			return Jwts.parser().setSigningKeyResolver(keyResolver).build().parseClaimsJws(token.replace("\"", "")).getBody();
		} catch (Exception e) {
			final String errorMessage = "Unable to validate the access token - " + token;
			logger.error(errorMessage);
			return null;
		}
	}
	
	
	public static Claims parseRSAJwtToken(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			String jwt = headerAuth.substring(7, headerAuth.length());
			if(validateRSAJwtToken(jwt)) {
				return parseRSAJwtToken(jwt);
			}
		}
		return null;
	}
	

	public static Claims getClaims(String token) {
		return Jwts.parser().setSigningKey(ConstantValues.KEYSECRET).build().parseClaimsJws(token).getBody();
	}

	public static boolean validateRSAJwtToken(String authToken) {
		try {
			Jwts.parser().setSigningKeyResolver(keyResolver).build().parseClaimsJws(authToken.replace("\"", ""));
			return true;
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		} catch (Exception e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}
}
