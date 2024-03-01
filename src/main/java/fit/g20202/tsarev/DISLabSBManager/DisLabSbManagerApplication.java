package fit.g20202.tsarev.DISLabSBManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RabbitConfiguration.class)
public class DisLabSbManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DisLabSbManagerApplication.class, args);
	}

}
