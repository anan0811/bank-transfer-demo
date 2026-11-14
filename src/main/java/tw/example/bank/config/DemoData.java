package tw.example.bank.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seedDemoAccounts(DemoDataInitializer initializer) {
        return args -> initializer.initialize();
    }
}
