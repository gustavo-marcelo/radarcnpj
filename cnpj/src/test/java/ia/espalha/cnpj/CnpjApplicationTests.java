package ia.espalha.cnpj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.import.enabled=false")
class CnpjApplicationTests {

	@Test
	void contextLoads() {
	}

}
