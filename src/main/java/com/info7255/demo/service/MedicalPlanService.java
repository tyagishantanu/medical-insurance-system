package com.info7255.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class MedicalPlanService {
    private final Jedis redisClient;
    private final ETagService eTagManager;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    public MedicalPlanService(Jedis redisClient, ETagService eTagManager) {
        this.redisClient = redisClient;
        this.eTagManager = eTagManager;
    }

    public boolean checkIfKeyExists(String key) {
        Map<String, String> data = redisClient.hgetAll(key);
        redisClient.close();
        return !(data == null || data.isEmpty());
    }

    public String fetchETag(String key) {
        return redisClient.hget(key, "eTag");
    }

    public String updateETag(String key, JSONObject planData) {
        String eTag = eTagManager.getETag(planData);
        redisClient.hset(key, "eTag", eTag);
        return eTag;
    }

    public String addNewPlan(JSONObject planDetails, String planId) {
        convertJsonToRedisData(planDetails);
        return updateETag(planId, planDetails);
    }

    public Map<String, Object> retrievePlanDetails(String planId) {
        Map<String, Object> planData = new HashMap<>();
        managePlanData(planId, planData, false);
        return planData;
    }

    public void removePlan(String planId) {
        managePlanData(planId, null, true);
    }

    private Map<String, Map<String, Object>> convertJsonToRedisData(JSONObject planJson) {
        Map<String, Map<String, Object>> redisData = new HashMap<>();
        Map<String, Object> planAttributes = new HashMap<>();

        for (String key : planJson.keySet()) {
            String composedKey = planJson.get("objectType") + ":" + planJson.get("objectId");
            Object value = planJson.get(key);

            if (value instanceof JSONObject) {
                value = convertJsonToRedisData((JSONObject) value);
                redisClient.sadd(composedKey + ":" + key, ((Map<String, Map<String, Object>>) value).entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                value = convertJsonArrayToList((JSONArray) value);
                ((List<Map<String, Map<String, Object>>>) value)
                        .forEach(item -> item.keySet()
                                .forEach(listKey -> redisClient.sadd(composedKey + ":" + key, listKey)));
            } else {
                redisClient.hset(composedKey, key, value.toString());
                planAttributes.put(key, value);
                redisData.put(composedKey, planAttributes);
            }
        }
        return redisData;
    }

    private Map<String, Object> managePlanData(String redisKey, Map<String, Object> resultData, boolean deleteFlag) {
        Set<String> relatedKeys = redisClient.keys(redisKey + ":*");
        relatedKeys.add(redisKey);

        for (String key : relatedKeys) {
            if (key.equals(redisKey)) {
                if (deleteFlag) redisClient.del(key);
                else {
                    Map<String, String> objectData = redisClient.hgetAll(key);
                    objectData.forEach((attrKey, value) -> {
                        if (!"eTag".equalsIgnoreCase(attrKey)) {
                            resultData.put(attrKey, isNumeric(value) ? Integer.parseInt(value) : value);
                        }
                    });
                }
            } else {
                manageNestedData(key, redisKey, resultData, deleteFlag);
            }
        }
        return resultData;
    }

    private void manageNestedData(String key, String redisKey, Map<String, Object> resultMap, boolean deleteFlag) {
        String attributeKey = key.substring((redisKey + ":").length());
        Set<String> members = redisClient.smembers(key);
        if (members.size() > 1 || "linkedPlanServices".equals(attributeKey)) {
            List<Object> listObjects = new ArrayList<>();
            for (String member : members) {
                if (deleteFlag) {
                    managePlanData(member, null, true);
                } else {
                    Map<String, Object> nestedData = new HashMap<>();
                    listObjects.add(managePlanData(member, nestedData, false));
                }
            }
            if (!deleteFlag) resultMap.put(attributeKey, listObjects);
        } else {
            manageSingleMemberData(members, key, resultMap, attributeKey, deleteFlag);
        }
        if (deleteFlag) redisClient.del(key);
    }

    private void manageSingleMemberData(Set<String> members, String key, Map<String, Object> resultMap, String attributeKey, boolean deleteFlag) {
        if (deleteFlag) {
            redisClient.del(members.iterator().next(), key);
        } else {
            Map<String, String> memberData = redisClient.hgetAll(members.iterator().next());
            Map<String, Object> nestedData = new HashMap<>();
            memberData.forEach((attrKey, value) -> nestedData.put(attrKey, isNumeric(value) ? Integer.parseInt(value) : value));
            resultMap.put(attributeKey, nestedData);
        }
    }

    public List<Object> convertJsonArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for (Object item : array) {
            if (item instanceof JSONArray) item = convertJsonArrayToList((JSONArray) item);
            else if (item instanceof JSONObject) item = convertJsonToRedisData((JSONObject) item);
            list.add(item);
        }
        return list;
    }

    private boolean isNumeric(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public List<Map<String, Object>> fetchAllPlans() {
        List<Map<String, Object>> plans = new ArrayList<>();
        Set<String> planKeys = redisClient.keys("plan:*");
        planKeys.forEach(key -> {
            try {
                if ("hash".equals(redisClient.type(key))) {
                    Map<String, Object> planData = retrievePlanDetails(key);
                    plans.add(planData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return plans;
    }
}
