package ia.espalha.cnpj.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import ia.espalha.cnpj.usuario.Usuario;

@Service
public class JwtService {

	private final SecretKey chave;
	private final long expiracaoMillis;

	public JwtService(
			@Value("${app.jwt.secret}") String segredo,
			@Value("${app.jwt.expiration-seconds:86400}") long expiracaoSegundos) {
		this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
		this.expiracaoMillis = expiracaoSegundos * 1000;
	}

	public String gerarToken(Usuario usuario) {
		Date agora = new Date();
		return Jwts.builder()
				.subject(usuario.email())
				.claim("papel", usuario.papel())
				.issuedAt(agora)
				.expiration(new Date(agora.getTime() + expiracaoMillis))
				.signWith(chave)
				.compact();
	}

	public Optional<String> emailDoToken(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(chave)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.ofNullable(claims.getSubject());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}