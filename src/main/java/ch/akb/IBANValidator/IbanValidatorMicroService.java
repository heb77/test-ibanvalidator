package ch.akb.IBANValidator;

import org.apache.commons.validator.routines.IBANValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IbanValidatorMicroService {

    public static void main(String[] args) {
        SpringApplication.run(IbanValidatorMicroService.class, args);
    }

    @Bean
    public IBANValidator ibanValidator(){
        return new IBANValidator();
    }

}
