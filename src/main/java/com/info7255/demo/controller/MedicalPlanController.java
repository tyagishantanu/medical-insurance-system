package com.info7255.demo.controller;

import com.info7255.demo.MedicalPlanApplication;
import com.info7255.demo.exception.BadRequestException;
import com.info7255.demo.exception.ConflictException;
import com.info7255.demo.exception.ETagParseException;
import com.info7255.demo.exception.ResourceNotFoundException;
import com.info7255.demo.model.ErrorResponse;
import com.info7255.demo.service.MedicalPlanService;
import com.info7255.demo.validator.JsonValidator;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class MedicalPlanController {

    @Autowired
    private MedicalPlanService medicalPlanService;

    @Autowired
    private JsonValidator validator;

    @Autowired
    private final RabbitTemplate template;

    public MedicalPlanController(MedicalPlanService medicalPlanService, RabbitTemplate template) {
        this.medicalPlanService = medicalPlanService;
        this.template = template;
    }

    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan(@RequestBody(required = false) String medicalPlan) {
        if (Objects.isNull(medicalPlan) || medicalPlan.isEmpty()) {
            throw new BadRequestException("Request body is missing! Kindly provide the JSON.");
        }

        JSONObject plan = new JSONObject(medicalPlan);
        try {
            validator.validateJsonSchema(plan);
        } catch (ValidationException e) {
            throw new BadRequestException("Bad Request: Json not validated");
        }

        String key = "plan:" + plan.getString("objectId");
        if (medicalPlanService.checkIfKeyExists(key)) {
            throw new ConflictException("Plan with this key already exists.");
        }

        String eTag = medicalPlanService.addNewPlan(plan, key);

        // Send a message to queue for indexing
        Map<String, String> message = new HashMap<>();
        message.put("operation", "SAVE");
        message.put("body", medicalPlan);

        System.out.println("Sending message: " + message);
        template.convertAndSend(MedicalPlanApplication.queueName, message);


        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);

        return new ResponseEntity<>("{\"objectId\": \"" + plan.getString("objectId") + "\"}", headersToSend, HttpStatus.CREATED);
    }

    @GetMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan(@PathVariable String objectId,
                                     @PathVariable String objectType,
                                     @RequestHeader HttpHeaders headers) {
        String key = objectType + ":" + objectId;
        if (!medicalPlanService.checkIfKeyExists(key)) {
            throw new ResourceNotFoundException("Plan not found with the specified key!");
        }

        List<String> ifNoneMatch;
        try {
            ifNoneMatch = headers.getIfNoneMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        String eTag = medicalPlanService.fetchETag(key);

        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);

        if (objectType.equals("plan") && ifNoneMatch.contains(eTag)) {
            return new ResponseEntity<>(null, headersToSend, HttpStatus.NOT_MODIFIED);
        }

        Map<String, Object> retrievedMedicalPlan = medicalPlanService.retrievePlanDetails(key);

        if (objectType.equals("plan")) {
            return new ResponseEntity<>(retrievedMedicalPlan, headersToSend, HttpStatus.OK);
        }

        return new ResponseEntity<>(retrievedMedicalPlan, HttpStatus.OK);
    }

    @GetMapping(value="/plan")
    public ResponseEntity<?> getAllPlans() {
        List<Map<String, Object>> medicalPlans = medicalPlanService.fetchAllPlans();
        if(medicalPlans.isEmpty()) {
            return new ResponseEntity<>("No Plans Found", HttpStatus.OK);
        }
        return new ResponseEntity<>(medicalPlans, HttpStatus.OK);
    }

    @DeleteMapping("/{objectType}/{objectId}")
    public ResponseEntity<?> deletePlan(@PathVariable String objectId,
                                        @PathVariable String objectType) {
        String key = objectType + ":" + objectId;
        if (!medicalPlanService.checkIfKeyExists(key)) {
            throw new ResourceNotFoundException("Plan not found with the specified key!");
        }

        // Send message to queue for deleting indices
        Map<String, Object> plan = medicalPlanService.retrievePlanDetails(key);
        Map<String, String> message = new HashMap<>();
        message.put("operation", "DELETE");
        message.put("body",  new JSONObject(plan).toString());

        System.out.println("Sending message: " + message);
        template.convertAndSend(MedicalPlanApplication.queueName, message);

        medicalPlanService.removePlan(key);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patchPlan(@PathVariable String objectId,
                                       @RequestBody(required = false) String planObject,
                                       @RequestHeader HttpHeaders headers ) {
        if (planObject == null || planObject.isEmpty()) throw new BadRequestException("Request body is missing!");

        JSONObject plan = new JSONObject(planObject);
        try {
            validator.validateJsonSchema(plan);
        } catch (ValidationException e) {
            throw new BadRequestException("Bad Request: Json not validated");
        }

        String key = "plan:" + objectId;
        if (!medicalPlanService.checkIfKeyExists(key)) throw new ResourceNotFoundException("Plan not found!");

        String eTag = medicalPlanService.fetchETag(key);
        List<String> ifMatch;
        try {
            ifMatch = headers.getIfMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        if (ifMatch.isEmpty()) {
            throw new ETagParseException("ETag is not provided with request!");
        }
        if (!ifMatch.contains(eTag)) {
            return preConditionFailed(eTag);
        }


        String updatedEtag = medicalPlanService.addNewPlan(plan, key);

        // Send message to queue for index update
        Map<String, String> message = new HashMap<>();
        message.put("operation", "SAVE");
        message.put("body", planObject);

        System.out.println("Sending message: " + message);
        template.convertAndSend(MedicalPlanApplication.queueName, message);

        return ResponseEntity.ok()
                .eTag(updatedEtag)
                .body(new JSONObject().put("message: ", "Plan updated successfully!!").toString());
    }

    private ResponseEntity preConditionFailed(String eTag) {
        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);
        ErrorResponse errorResponse = new ErrorResponse(
                "Precondition failed",
                HttpStatus.PRECONDITION_FAILED.value(),
                new Date(),
                HttpStatus.PRECONDITION_REQUIRED.getReasonPhrase()
        );
        return new ResponseEntity<>(errorResponse, headersToSend, HttpStatus.PRECONDITION_FAILED);
    }

}
