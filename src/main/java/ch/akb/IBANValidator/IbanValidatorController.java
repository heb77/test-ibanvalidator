package ch.akb.IBANValidator;

import java.time.Duration;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.validator.routines.IBANValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import ch.akb.IBANValidator.objects.ValidationBody;
import ch.akb.IBANValidator.objects.ValidationResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//@CrossOrigin(origins = "http://localhost:8085")
@Slf4j
@RequestMapping("/iban")
@RestController
public class IbanValidatorController {

	private static final long REQUEST_TIMEOUT_MS = 5000;
	private static final String port = "8081";
	private String asbMuleServer = "localhost";
//	private String asbMuleServer = "sit-asb.its.agkb.ch";

	@Autowired
	IBANValidator ibanValidator;
	
	@CrossOrigin
	@GetMapping()
	ResponseEntity<IbanValidatorResponse> showInfoHome() {
		log.info("started ...");
		IbanValidatorResponse response = new IbanValidatorResponse();
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).withSelfRel());
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).slash("CH2208401042047114400").withRel("validate_iban"));
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).slash("status").withRel("status"));
		log.info("finished -> " + response);
		return new ResponseEntity<IbanValidatorResponse>(response, HttpStatus.OK);
	}
	
	@GetMapping(value="/{iban}")
	ResponseEntity<ValidationResult> getIban(@PathVariable("iban") String iban) throws Exception {
		log.info("started ... iban:" + iban);
		ValidationResult response = validateIban(iban);
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).slash(iban).withSelfRel());
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).withRel("home"));
		log.info("finished -> " + response);
		return new ResponseEntity<ValidationResult>(response, HttpStatus.OK);
	}	

	@PostMapping(path = "/validate", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@Deprecated
	public ValidationResult validateIban(@Valid @RequestBody ValidationBody requestBody) {
		return validateIban(requestBody.getIban());
	}
		
	@GetMapping(path = "/status")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<IbanValidatorResponse>  getStatus() {
		log.info("getStatus started ...");
		IbanValidatorResponse response = new IbanValidatorResponse();
		response.add(WebMvcLinkBuilder.linkTo(IbanValidatorController.class).withRel("home"));
		log.info("finished -> " + response);
		return new ResponseEntity<IbanValidatorResponse>(response, HttpStatus.OK);
	}

	public ValidationResult validateIban(String iban) {
		log.info("validateIban started ... iban : " + iban );
		ValidationResult vr = new ValidationResult();
		vr.setIban(iban);
		extractIban(vr);
		validateIban(vr);
		extractClearingNr(vr);
		lookupBP(vr);
		lookupBankName(vr);
		log.info("result : " + vr);
		return vr;
	}

	private void validateIban(ValidationResult vr) {
		vr.setValid(ibanValidator.isValid(vr.getIban()));
		log.info("iban validator : " + vr.isValid());
	}

	/**
	 * @param vr
	 * @return iban , spaces removed
	 */
	private void extractIban(ValidationResult vr) {
		String iban = vr.getIban();
		log.info("requested Iban : '" + iban + "'");
		iban = iban.replace(" ", ""); // remove spaces
		log.info("requested Iban length: " + iban.length());
		if (iban.length() != 21) {
			String msg = "only IBAN-21 supported , iban was : " + vr.getIban() + " , length : " + vr.getIban().length();
			log.error(msg);
			vr.getErrors().add(msg);
		}
		vr.setIban(iban);
	}

	// CH66*00769*0160988886401
	private ValidationResult extractClearingNr(ValidationResult vr) {
		try {
			String clearingNr = vr.getIban().substring(4, 9);
			log.info("clearingNr : " + clearingNr);
			vr.setClearingNummer(clearingNr);
		} catch (Exception e) {
			String msg = "could not extract clearing nr in iban : " + vr.getIban() + ", exception : " + e;
			log.error(msg);
			vr.getErrors().add(msg);
		}
		return vr;
	}

	private void lookupBP(ValidationResult vr) {
		log.info("start lookup BP ...");
		final String uri = "/s-avaloq/search";
		try {
			WebClient webclient = WebClient.builder().baseUrl(getBaseUrl()).build();
			JSONObject body = new JSONObject();
			JSONObject businessPartner = new JSONObject();
			businessPartner.put("clearingNumber", vr.getClearingNummer());
			body.put("businessPartner", businessPartner);

			JSONObject responseObj = new JSONObject();
			JSONArray fields = new JSONArray();
			fields.put("businessPartnerIds");

			responseObj.put("fields", fields);
			body.put("response", responseObj);

			String bodyJsonStr = body.toString(1);
			log.info("bodyJsonStr : " + bodyJsonStr);

			Duration timeout = Duration.ofMillis(REQUEST_TIMEOUT_MS);
			ResponseSpec responseSpec = webclient.post().uri(uri)
					.header("Content-Type", "application/vnd.akb.api.search-query-v1+json")
					.header("Accept", "application/vnd.akb.api.search-response-v1+json").bodyValue(bodyJsonStr)
					.retrieve().onStatus(status -> status.value() >= 400, clientResponse -> Mono.empty());

			ResponseEntity<SearchResponse> response = responseSpec.toEntity(SearchResponse.class).block(timeout);

			log.info("response : " + response);
			log.info("response status : " + response.getStatusCodeValue());

			SearchResponse searchResponse = response.getBody();
			String[] bps = searchResponse.getBusinessPartnerIds();
			log.info("searchResponse : " + searchResponse);

			if ((response.getStatusCodeValue() >= 400) || (bps == null) || (bps.length != 1)) {
				String msg = "could not lookup bpid with iban : " + vr.getIban() + " response :" + response;
				log.error(msg);
				vr.getErrors().add(msg);
			} else {
				vr.setBusinnesPartnerId(bps[0]);
			}
		} catch (Exception e) {
			String msg = "could not lookup bp with iban : " + vr.getIban() + " , url : " + getBaseUrl() + uri + " , exception : " + e;
			log.error(msg, e);
			vr.getErrors().add(msg);
		}
	}

	private void lookupBankName(ValidationResult vr) {
		log.info("lookup BankName started ...");
		final String uri = "/s-avaloq/businessPartners/" + vr.getBusinnesPartnerId();
		try {
			WebClient webclient = WebClient.builder().baseUrl(getBaseUrl()).build();
			ResponseSpec responseSpec = webclient.get().uri(uri)
					.header("Accept", "application/vnd.akb.api.bp-v1+json").retrieve()
					.onStatus(status -> status.value() >= 400, clientResponse -> Mono.empty());

			Duration timeout = Duration.ofMillis(REQUEST_TIMEOUT_MS);
			ResponseEntity<Map> responseEntity = responseSpec.toEntity(Map.class).block(timeout);
			log.info("responseEntity : " + responseEntity);
			log.info("response status : " + responseEntity.getStatusCodeValue());

			if (responseEntity.getStatusCodeValue() >= 400) {
				String msg = "could not lookup BankName with iban : " + vr.getIban() + " , http response : "  + responseEntity;
				log.error(msg);
				vr.getErrors().add(msg);
			} else {
				Map response = responseEntity.getBody();
				String name = (String) response.get("name");
				log.info("name : " + name);
				vr.setBankName(name);
			}
		} catch (Exception e) {
			String msg = "could not lookup bankname with iban : " + vr.getIban() + " , url : " + getBaseUrl() + uri + " , exception : " + e;
			log.error(msg, e);
			vr.getErrors().add(msg);
		}
	}

	public String getBaseUrl() {
		return "http://" + asbMuleServer + ":" + port;
	}
}

@Data
class IbanValidatorResponse extends RepresentationModel<IbanValidatorResponse> {
}

@Data
class SearchResponse {
	String[] businessPartnerIds;
}
