package net.whydah.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "net.whydah")
public class ApplicationConfig {
    // This class ensures Spring properly scans and configures all components
}