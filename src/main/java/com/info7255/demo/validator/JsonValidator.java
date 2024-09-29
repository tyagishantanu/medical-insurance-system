package com.info7255.demo.validator;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class JsonValidator {
    public void validateJsonSchema(JSONObject object) throws ValidationException {
        try(InputStream inputStream = getClass().getResourceAsStream("/plan-schema.json")) {
            JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(schemaJson);
            schema.validate(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
