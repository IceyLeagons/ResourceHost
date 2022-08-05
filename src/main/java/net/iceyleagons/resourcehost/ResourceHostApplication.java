package net.iceyleagons.resourcehost;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

@SpringBootApplication
public class ResourceHostApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceHostApplication.class, args);
    }

}
