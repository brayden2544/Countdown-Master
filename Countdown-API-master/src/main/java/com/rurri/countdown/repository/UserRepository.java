package com.rurri.countdown.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.rurri.countdown.model.MatchQueuePointer;
import com.rurri.countdown.model.User;
import com.rurri.countdown.service.CacheService;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.WebServiceException;
import java.util.*;
import java.util.concurrent.Future;

public class UserRepository {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    Provider<DynamoDBMapper> mapperProvider;

    @Inject
    CacheService cache;

    @Inject
    Provider<AmazonDynamoDBAsync> amazonDynamoProvider;

    @Inject
    Config config;

    public User getUser(long userId) {
        User user = cache.getUser(userId);
        if (user == null) {
            DynamoDBMapper mapper = mapperProvider.get();
            user = mapper.load(User.class, userId);
            if (user != null) {
                cache.cacheUser(user);
            }
        }
        return user;
    }

    @SuppressWarnings("unchecked")
    public Collection<User> getUsers(List<Long> userIds) {

        LinkedHashMap<Long, User> users = cache.getUsers(userIds);
        DynamoDBMapper mapper = mapperProvider.get();

        List userLookups = new ArrayList<User>(users.size());
        for(Long userId : userIds) {
            if (users.get(userId) == null) {
                User user = new User();
                user.setUserId(userId);
                userLookups.add(user);
            }
        }

        if (userLookups.size() > 0) {
            Map<String, List<Object>> batchResults = mapper.batchLoad(userLookups);
            List userResults = batchResults.get(config.getString("dynamodb.tablePrefix") + "User");
            cache.cacheUsers((List<User>)userResults);
            for (User user : (List<User>)userResults) {
                users.put(user.getUserId(), user);
            }
        }
        return users.values();
    }

    public Future<QueryResult> getUserResultsByRegion(User user, String geoRegion, Map<String, AttributeValue> lastEvaluatedKey, int limit) {

        int precision = geoRegion.length();

        Map<String, Condition> keyConditions = new HashMap<String, Condition>();

        {
            AttributeValue attributeValue = new AttributeValue();
            attributeValue.setN(Integer.toString(User.FEMALE_BITMASK));
            Collection<AttributeValue> attributeValueList = new ArrayList<AttributeValue>(1);
            attributeValueList.add(attributeValue);

            Condition condition = new Condition();
            condition.setAttributeValueList(attributeValueList);
            if (user.getGender().equals("M")) {
                condition.setComparisonOperator(ComparisonOperator.GE);
            } else {
                condition.setComparisonOperator(ComparisonOperator.LT);
            }
            keyConditions.put("g-pts", condition);
        }

        {
            AttributeValue attributeValue = new AttributeValue();
            attributeValue.setS(geoRegion);
            Collection<AttributeValue> attributeValueList = new ArrayList<AttributeValue>(1);
            attributeValueList.add(attributeValue);

            Condition condition = new Condition();
            condition.setAttributeValueList(attributeValueList);
            condition.setComparisonOperator(ComparisonOperator.EQ);
            keyConditions.put("geo" + precision, condition);
        }

        QueryRequest queryRequest = new QueryRequest(config.getString("dynamodb.tablePrefix") + "User");
        queryRequest.setIndexName("UserGeo" + precision);
        queryRequest.setKeyConditions(keyConditions);
        queryRequest.setScanIndexForward(false);
        queryRequest.setLimit(limit);
        if (lastEvaluatedKey != null) {
            queryRequest.setExclusiveStartKey(lastEvaluatedKey);
        }

        AmazonDynamoDBAsync dynamoDB = amazonDynamoProvider.get();
        return dynamoDB.queryAsync(queryRequest);
    }

    public Future<QueryResult> getMatchQueuePointer(User user, String geoRegion) {

        Map<String, Condition> keyConditions = new HashMap<String, Condition>();

        {
            AttributeValue attributeValue = new AttributeValue();
            attributeValue.setS(geoRegion);
            Collection<AttributeValue> attributeValueList = new ArrayList<AttributeValue>(1);
            attributeValueList.add(attributeValue);

            Condition condition = new Condition();
            condition.setAttributeValueList(attributeValueList);
            condition.setComparisonOperator(ComparisonOperator.EQ);

            keyConditions.put("geo", condition);
        }

        {
            AttributeValue attributeValue = new AttributeValue();
            attributeValue.setN(Long.toString(user.getUserId()));
            Collection<AttributeValue> attributeValueList = new ArrayList<AttributeValue>(1);
            attributeValueList.add(attributeValue);

            Condition condition = new Condition();
            condition.setAttributeValueList(attributeValueList);
            condition.setComparisonOperator(ComparisonOperator.EQ);
            keyConditions.put("uid", condition);
        }

        QueryRequest queryRequest = new QueryRequest(config.getString("dynamodb.tablePrefix") + "MatchQueuePointer");
        queryRequest.setKeyConditions(keyConditions);
        AmazonDynamoDBAsync dynamoDB = amazonDynamoProvider.get();
        return dynamoDB.queryAsync(queryRequest);
    }

    @SuppressWarnings("unchecked")
    public Collection<MatchQueuePointer> getMatchQueuePointers(User user, List<String> geoCodes) {

        DynamoDBMapper mapper = mapperProvider.get();
        LinkedHashMap<String, MatchQueuePointer> matchQueues = new LinkedHashMap<String, MatchQueuePointer>();
        List geoLookups = new ArrayList<MatchQueuePointer>(geoCodes.size());
        for(String geoCode : geoCodes) {
            MatchQueuePointer matchQueuePointer = new MatchQueuePointer();
            matchQueuePointer.setUserId(user.getUserId());
            matchQueuePointer.setGeoHash(geoCode);
            geoLookups.add(matchQueuePointer);
            matchQueues.put(geoCode, matchQueuePointer);
        }

        Map<String, List<Object>> batchResults = mapper.batchLoad(geoLookups);
        for(Object matchQueuePointer : batchResults.get(config.getString("dynamodb.tablePrefix") + "MatchQueuePointer")) {
            matchQueues.put(((MatchQueuePointer) matchQueuePointer).getGeoHash(), (MatchQueuePointer)matchQueuePointer);
        }
        return matchQueues.values();
    }

    public void saveMatchQueuePointers(Collection<MatchQueuePointer> pointers) {
        DynamoDBMapper mapper = mapperProvider.get();
        for(MatchQueuePointer pointer : pointers) {
            mapper.save(pointer);
        }
    }

    public List<String> getExludeList(long userId) {

        AmazonDynamoDBAsync dynamoDB = amazonDynamoProvider.get();

        /*
         * Next build query for Pass List
         */
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();

        AttributeValue attributeValue = new AttributeValue();
        attributeValue.setN(Long.toString(userId));
        Collection<AttributeValue> attributeValueList = new ArrayList<AttributeValue>(1);
        attributeValueList.add(attributeValue);

        Condition condition = new Condition();
        condition.setAttributeValueList(attributeValueList);
        condition.setComparisonOperator(ComparisonOperator.EQ);
        keyConditions.put("uid", condition);

        QueryRequest queryRequest = new QueryRequest(config.getString("dynamodb.tablePrefix") + "Pass");
        queryRequest.setKeyConditions(keyConditions);

        Future<QueryResult> passQuery = dynamoDB.queryAsync(queryRequest);

        /*
         * Next build query for Like List
         */

        keyConditions = new HashMap<String, Condition>();

        attributeValue = new AttributeValue();
        attributeValue.setN(Long.toString(userId));
        attributeValueList = new ArrayList<AttributeValue>(1);
        attributeValueList.add(attributeValue);

        condition = new Condition();
        condition.setAttributeValueList(attributeValueList);
        condition.setComparisonOperator(ComparisonOperator.EQ);
        keyConditions.put("uid", condition);

        queryRequest = new QueryRequest(config.getString("dynamodb.tablePrefix") + "Like");
        queryRequest.setKeyConditions(keyConditions);

        Future<QueryResult> likeQuery = dynamoDB.queryAsync(queryRequest);

        try {
            List<String> excludeIds = new ArrayList<String>();

            /*
             * Parse Pass Query
             */
            for(Map<String, AttributeValue> resultRow : passQuery.get().getItems()) {
                excludeIds.add(resultRow.get("pid").getN());
            }

            /*
             * Parse Match Query
             */

            for(Map<String, AttributeValue> resultRow : likeQuery.get().getItems()) {
                excludeIds.add(resultRow.get("lid").getN());
            }

            return excludeIds;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error building exclude List for user", e);
            throw new WebServiceException(e);
        }
    }

    public void saveUser(User user) {
        mapperProvider.get().save(user);
        cache.cacheUser(user);
    }
}
