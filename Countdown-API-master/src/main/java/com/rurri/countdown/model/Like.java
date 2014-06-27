package com.rurri.countdown.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by jason on 5/6/14.
 */
@DynamoDBTable(tableName="Like")
public class Like {

    @DynamoDBHashKey(attributeName="uid")
    @JsonProperty("uid")
    private long userId;

    @DynamoDBRangeKey(attributeName = "lid")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "Liked")
    @JsonProperty("lid")
    private long likedUserId;

    @DynamoDBAttribute(attributeName = "recip")
    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "Liked")
    private int reciprocal = 1;

    @DynamoDBAttribute(attributeName="likedAt")
    private Date likedAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getLikedUserId() {
        return likedUserId;
    }

    public void setLikedUserId(long likedUserId) {
        this.likedUserId = likedUserId;
    }

    public int getReciprocal() {
        return reciprocal;
    }

    public void setReciprocal(int reciprocal) {
        this.reciprocal = reciprocal;
    }

    public Date getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(Date likedAt) {
        this.likedAt = likedAt;
    }

    @DynamoDBIgnore
    public static Collection<GlobalSecondaryIndex> getIndexes() {
        Collection<GlobalSecondaryIndex> indexes = new ArrayList<GlobalSecondaryIndex>(3);
        {
            GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex();
            secondaryIndex.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            secondaryIndex.setIndexName("Liked");

            KeySchemaElement hashElement = new KeySchemaElement("lid", KeyType.HASH);
            KeySchemaElement rangeElement = new KeySchemaElement("recip", KeyType.RANGE);
            Collection<KeySchemaElement> indexKeys = new ArrayList<KeySchemaElement>(2);
            indexKeys.add(hashElement);
            indexKeys.add(rangeElement);
            secondaryIndex.setKeySchema(indexKeys);

            Projection projection = new Projection();
            projection.setProjectionType(ProjectionType.INCLUDE);
            Collection<String> projectAttributes = new ArrayList<String>(2);
            projectAttributes.add("recip");
            projection.setNonKeyAttributes(projectAttributes);
            secondaryIndex.setProjection(projection);

            indexes.add(secondaryIndex);
        }
        return indexes;
    }

}
