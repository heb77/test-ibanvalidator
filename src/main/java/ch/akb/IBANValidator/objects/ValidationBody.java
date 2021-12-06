package ch.akb.IBANValidator.objects;

import javax.validation.constraints.NotNull;

public class ValidationBody {

    @NotNull
    private String iban;

    public ValidationBody() {
    }

    public ValidationBody(String iban) {
        this.iban = iban;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    @Override
    public String toString() {
        return "ValidationBody{" +
                "iban='" + iban + '\'' +
                '}';
    }
}
