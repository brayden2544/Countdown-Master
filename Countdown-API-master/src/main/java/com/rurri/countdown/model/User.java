package com.rurri.countdown.model;

import ch.hsr.geohash.GeoHash;
import com.amazonaws.services.dynamodbv2.datamodeling.*;

import java.io.IOException;
import java.security.Key;
import java.util.*;

import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jason on 5/6/14.
 */

@DynamoDBTable(tableName="User")
public class User {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int MALE_BITMASK = 0;
    public static final int FEMALE_BITMASK = 128;

    public enum Gender {
        MALE, FEMALE
    }

    @DynamoDBHashKey(attributeName="uid")
    @JsonProperty("uid")
    private long userId;

    @DynamoDBAttribute(attributeName="pts")
    private Integer points;

    @DynamoDBAttribute(attributeName="geo")
    private String geoHash;

    @DynamoDBAttribute(attributeName="geo4")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "UserGeo4")
    private String fourDigitGeohash;

    @DynamoDBAttribute(attributeName="geo5")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "UserGeo5")
    private String fiveDigitGeohash;

    @DynamoDBAttribute(attributeName="lat")
    @JsonProperty("lat")
    private Float latitude;

    @DynamoDBAttribute(attributeName="long")
    @JsonProperty("long")
    private Float longitude;

    @DynamoDBAttribute(attributeName="sex")
    private String gender;

    @DynamoDBAttribute(attributeName="fname")
    private String firstName;

    @DynamoDBAttribute(attributeName="lname")
    private String lastName;

    @DynamoDBAttribute(attributeName="dob")
    private Date dateOfBirth;

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "UserGeo4,UserGeo5")
    @DynamoDBAttribute(attributeName="g-pts")
    private Integer genderPoints;

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "UserLastShown")
    @DynamoDBAttribute(attributeName="lshown")
    private Date lastShown;

    @DynamoDBAttribute(attributeName="pic")
    private String picUri;

    @DynamoDBAttribute(attributeName="vid")
    private String videoUri;

    @DynamoDBAttribute(attributeName="token")
    private String lastAccessToken;

    @DynamoDBAttribute(attributeName="loc")
    private String locale;

    @DynamoDBAttribute(attributeName="tz")
    private String timeZone;

    @DynamoDBAttribute(attributeName="twit_un")
    @JsonProperty("twitter_username")
    private String twitterUsername;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
        this.calculateGenderPoints();
    }

    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
        this.calculateGenderPoints();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getGenderPoints() {
        return genderPoints;
    }

    public void setGenderPoints(Integer genderPoints) {
        this.genderPoints = genderPoints;
    }

    public Date getLastShown() {
        return lastShown;
    }

    public void setLastShown(Date lastShown) {
        this.lastShown = lastShown;
    }

    public String getPicUri() {
        return picUri;
    }

    public void setPicUri(String picUri) {
        this.picUri = picUri;
    }

    public String getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(String videoUri) {
        this.videoUri = videoUri;
        this.calculateGenderPoints();
    }

    public String getLastAccessToken() {
        return lastAccessToken;
    }

    public void setLastAccessToken(String lastAccessToken) {
        this.lastAccessToken = lastAccessToken;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getFourDigitGeohash() {
        return fourDigitGeohash;
    }

    public void setFourDigitGeohash(String fourDigitGeohash) {
        this.fourDigitGeohash = fourDigitGeohash;
    }

    public String getFiveDigitGeohash() {
        return fiveDigitGeohash;
    }

    public void setFiveDigitGeohash(String fiveDigitGeohash) {
        this.fiveDigitGeohash = fiveDigitGeohash;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    public String getTwitterUsername() {
        return twitterUsername;
    }

    public void setTwitterUsername(String twitterUsername) {
        this.twitterUsername = twitterUsername;
    }

    public void setLocation(Float latitude, Float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.geoHash = GeoHash.withBitPrecision(latitude, longitude, 55).toBase32();
        this.fourDigitGeohash = this.geoHash.substring(0, 4);
        this.fiveDigitGeohash = this.geoHash.substring(0, 5);
    }

    private void calculateGenderPoints() {
        if (this.points != null && this.gender != null && StringUtils.isNotBlank(this.videoUri)) {
            if (this.gender.equals("F")) {
                this.genderPoints = this.points | FEMALE_BITMASK;
            } else if (this.gender.equals("M")) {
                this.genderPoints = this.points;
            } else {
                this.genderPoints = null;
            }
        } else {
            this.genderPoints = null;
        }
    }


    @DynamoDBIgnore
    public static Collection<GlobalSecondaryIndex> getIndexes() {
        Collection<GlobalSecondaryIndex> indexes = new ArrayList<GlobalSecondaryIndex>(3);
        {
            GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex();
            secondaryIndex.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            secondaryIndex.setIndexName("UserGeo4");

            KeySchemaElement hashElement = new KeySchemaElement("geo4", KeyType.HASH);
            KeySchemaElement rangeElement = new KeySchemaElement("g-pts", KeyType.RANGE);
            Collection<KeySchemaElement> indexKeys = new ArrayList<KeySchemaElement>(2);
            indexKeys.add(hashElement);
            indexKeys.add(rangeElement);
            secondaryIndex.setKeySchema(indexKeys);

            Projection projection = new Projection();
            projection.setProjectionType(ProjectionType.INCLUDE);
            Collection<String> projectAttributes = new ArrayList<String>(2);
            projectAttributes.add("uid");
            projectAttributes.add("pic");
            projectAttributes.add("vid");
            projectAttributes.add("geo");
            projection.setNonKeyAttributes(projectAttributes);
            secondaryIndex.setProjection(projection);

            indexes.add(secondaryIndex);
        }

        {
            GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex();
            secondaryIndex.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            secondaryIndex.setIndexName("UserGeo5");

            KeySchemaElement hashElement = new KeySchemaElement("geo5", KeyType.HASH);
            KeySchemaElement rangeElement = new KeySchemaElement("g-pts", KeyType.RANGE);
            Collection<KeySchemaElement> indexKeys = new ArrayList<KeySchemaElement>(2);
            indexKeys.add(hashElement);
            indexKeys.add(rangeElement);
            secondaryIndex.setKeySchema(indexKeys);

            Projection projection = new Projection();
            projection.setProjectionType(ProjectionType.INCLUDE);
            Collection<String> projectAttributes = new ArrayList<String>(2);
            projectAttributes.add("uid");
            projectAttributes.add("pic");
            projectAttributes.add("vid");
            projectAttributes.add("geo");
            projection.setNonKeyAttributes(projectAttributes);
            secondaryIndex.setProjection(projection);

            indexes.add(secondaryIndex);
        }

        {
            GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex();
            secondaryIndex.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            secondaryIndex.setIndexName("UserLastShown");

            KeySchemaElement hashElement = new KeySchemaElement("geo4", KeyType.HASH);
            KeySchemaElement rangeElement = new KeySchemaElement("lshown", KeyType.RANGE);
            Collection<KeySchemaElement> indexKeys = new ArrayList<KeySchemaElement>(2);
            indexKeys.add(hashElement);
            indexKeys.add(rangeElement);
            secondaryIndex.setKeySchema(indexKeys);

            Projection projection = new Projection();
            projection.setProjectionType(ProjectionType.INCLUDE);
            Collection<String> projectAttributes = new ArrayList<String>(2);
            projectAttributes.add("uid");
            projectAttributes.add("pic");
            projectAttributes.add("vid");
            projectAttributes.add("geo");
            projection.setNonKeyAttributes(projectAttributes);
            secondaryIndex.setProjection(projection);

            indexes.add(secondaryIndex);
        }
        return indexes;
    }
}
