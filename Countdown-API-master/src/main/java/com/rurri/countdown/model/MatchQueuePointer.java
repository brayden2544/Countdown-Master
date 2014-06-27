package com.rurri.countdown.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@DynamoDBTable(tableName="MatchQueuePointer")
public class MatchQueuePointer {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @DynamoDBHashKey(attributeName = "uid")
    @JsonProperty("uid")
    private long userId;

    @DynamoDBRangeKey(attributeName = "geo")
    @JsonProperty("geo")
    private String geoHash;

    @DynamoDBAttribute(attributeName="lastEvalString")
    private String lastEvaluatedKeyString = null;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }

    public String getLastEvaluatedKeyString() {
        return lastEvaluatedKeyString;
    }

    public void setLastEvaluatedKeyString(String lastEvaluatedKeyString) {
        this.lastEvaluatedKeyString = lastEvaluatedKeyString;
    }

    @JsonIgnore
    @DynamoDBIgnore
    @SuppressWarnings("unchecked")
    public Map<String, AttributeValue> getLastEvaluatedKey() {
        if (this.lastEvaluatedKeyString == null) {
            return null;
        } else {
            ObjectMapper jacksonMapper = new ObjectMapper();
            jacksonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                return jacksonMapper.readValue(this.lastEvaluatedKeyString, new TypeReference<Map<String, AttributeValue>>(){});
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Unable to deserialize lastEval Key.", e);
                throw new RuntimeException("Unable to deseriale lastEval Key", e);
            }
        }
    }

    @JsonIgnore
    @DynamoDBIgnore
    public void setLastEvaluatedKey(Map<String, AttributeValue> lastEvaluatedKey) {
        ObjectMapper jacksonMapper = new ObjectMapper();
        jacksonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            this.lastEvaluatedKeyString = jacksonMapper.writeValueAsString(lastEvaluatedKey);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Unable to serialize lastEval key.", e);
            throw new RuntimeException("Unable to serialize lastEval Key", e);
        }
    }

}


