package juton113.Medicall;

import juton113.Medicall.global.config.SymptomRuleConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties(SymptomRuleConfig.class)
public class MedicallApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicallApplication.class, args);
	}

}
