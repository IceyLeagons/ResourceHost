package net.iceyleagons.resourcepackhost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication
public class ResourceHost {

	public static void main(String[] args) {
		SpringApplication.run(ResourceHost.class, args);
	}

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofMegabytes(21));
		factory.setMaxRequestSize(DataSize.ofMegabytes(21));
		return factory.createMultipartConfig();
	}
}
