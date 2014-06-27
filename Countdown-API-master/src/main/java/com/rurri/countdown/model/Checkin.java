package com.rurri.countdown.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@DynamoDBTable(tableName="Checkin")
public class CheckIn {

    @DynamoDBHashKey(attributeName = "uid")
    @JsonProperty("uid")
    private long userId;

    @DynamoDBRangeKey(attributeName = "geo")
    @JsonProperty("geo")
    private String geoHash;

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
}


