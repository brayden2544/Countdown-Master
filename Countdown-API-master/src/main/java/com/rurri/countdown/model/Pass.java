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
@DynamoDBTable(tableName="Pass")
public class Pass {

    @DynamoDBHashKey(attributeName="uid")
    @JsonProperty("uid")
    private long userId;

    @DynamoDBRangeKey(attributeName = "pid")
    @JsonProperty("lid")
    private long passedUserId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "Passed")
    private Date passedAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getPassedUserId() {
        return passedUserId;
    }

    public void setPassedUserId(long passedUserId) {
        this.passedUserId = passedUserId;
    }

    public Date getPassedAt() {
        return passedAt;
    }

    public void setPassedAt(Date passedAt) {
        this.passedAt = passedAt;
    }

    @DynamoDBIgnore
    public static Collection<GlobalSecondaryIndex> getIndexes() {
        Collection<GlobalSecondaryIndex> indexes = new ArrayList<GlobalSecondaryIndex>(3);
        {
            GlobalSecondaryIndex secondaryIndex = new GlobalSecondaryIndex();
            secondaryIndex.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
            secondaryIndex.setIndexName("Passed");

            KeySchemaElement hashElement = new KeySchemaElement("pid", KeyType.HASH);
            KeySchemaElement rangeElement = new KeySchemaElement("passedAt", KeyType.RANGE);
            Collection<KeySchemaElement> indexKeys = new ArrayList<KeySchemaElement>(2);
            indexKeys.add(hashElement);
            indexKeys.add(rangeElement);
            secondaryIndex.setKeySchema(indexKeys);

            Projection projection = new Projection();
            projection.setProjectionType(ProjectionType.KEYS_ONLY);
            secondaryIndex.setProjection(projection);

            indexes.add(secondaryIndex);
        }
        return indexes;
    }

}
