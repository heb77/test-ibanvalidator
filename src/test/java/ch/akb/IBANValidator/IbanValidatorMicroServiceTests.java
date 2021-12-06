package ch.akb.IBANValidator;

import ch.akb.IBANValidator.objects.ValidationBody;
import ch.akb.IBANValidator.objects.ValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IbanValidatorMicroServiceTests {

    @Autowired
    IbanValidatorController ibanValidatorController;

    @Test
    void checkIbanWithWhitespace() {
        ValidationResult vr = this.ibanValidatorController.validateIban(new ValidationBody("GB82 WEST 1234 5698 7654 32"));
        assertEquals(vr.isValid(), true);
    }

    @Test
    void checkIban() {
        ValidationResult vr = this.ibanValidatorController.validateIban(new ValidationBody("GB82WEST12345698765432"));
        assertEquals(vr.isValid(), true);
    }

    @Test
    void checkWrongIban() {
        ValidationResult vr = this.ibanValidatorController.validateIban(new ValidationBody("GB82W11112345698765432"));
        assertEquals(vr.isValid(), false);
    }

    @Test
    void checkWrongIbanWithInvalidCharacters() {
        ValidationResult vr = this.ibanValidatorController.validateIban(new ValidationBody("GB82W!@#$12345698765432"));
        assertEquals(vr.isValid(), false);
    }

}
