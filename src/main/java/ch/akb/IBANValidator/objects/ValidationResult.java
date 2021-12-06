package ch.akb.IBANValidator.objects;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ValidationResult extends RepresentationModel<ValidationResult> {
	
	private String iban;
	
    @NotNull
    private boolean isValid = false;

    private String clearingNummer = "";
	private String businnesPartnerId = "";
    private String bankName = "";
    
    private List<String> errors = new ArrayList<String>();    
}
