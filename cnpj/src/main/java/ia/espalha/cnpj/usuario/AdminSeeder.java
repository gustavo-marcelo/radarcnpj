package ia.espalha.cnpj.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final UsuarioRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String senha;

	public AdminSeeder(UsuarioRepository repository, PasswordEncoder passwordEncoder,
			@Value("${app.admin.email}") String email, @Value("${app.admin.senha}") String senha) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.senha = senha;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		repository.insert(new Usuario(
				null, "Administrador", email.toLowerCase(),
				passwordEncoder.encode(senha), "ADMIN", true, null, null));
		log.info("Usuário ADMIN inicial criado: {}", email);
	}
}