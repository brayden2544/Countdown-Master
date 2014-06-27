package com.rurri.countdown.resource;

/**
 * Created by jason on 5/6/14.
 */

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Inject;
import com.rurri.countdown.exception.AuthenticationException;
import com.rurri.countdown.model.*;
import com.rurri.countdown.service.AuthenticationService;
import com.typesafe.config.Config;
import org.jfairy.Fairy;
import org.jfairy.producer.person.Person;
import org.jfairy.producer.person.PersonProperties;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

;import java.util.*;

@Path("/setup")
public class InstallResource extends BaseResource {

    @Inject
    AuthenticationService authService;

    @Inject
    AmazonDynamoDB dynamoDBClient;

    @Inject
    DynamoDBMapper mapper;

    @Inject
    Config config;

    @POST
    @Path("/install")
    @Produces("text/plain")
    public String installApp(@HeaderParam("Access-Token") String accessToken, @Context final HttpServletResponse response) {

        long userId = authService.getUserId(accessToken);
        if (userId != 533081609) {
            throw new AuthenticationException("Not allowed");
        }

        logger.info("Creating All tables");

        CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(User.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(User.getIndexes());
        dynamoDBClient.createTable(createTableRequest);
        this.waitForTableToBecomeAvailable(createTableRequest.getTableName());


        createTableRequest = mapper.generateCreateTableRequest(Like.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(Like.getIndexes());
        dynamoDBClient.createTable(createTableRequest);
        this.waitForTableToBecomeAvailable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Pass.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(Pass.getIndexes());
        dynamoDBClient.createTable(createTableRequest);
        this.waitForTableToBecomeAvailable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Friend.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        dynamoDBClient.createTable(createTableRequest);

        createTableRequest = mapper.generateCreateTableRequest(Video.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        dynamoDBClient.createTable(createTableRequest);

        createTableRequest = mapper.generateCreateTableRequest(CheckIn.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        dynamoDBClient.createTable(createTableRequest);

        createTableRequest = mapper.generateCreateTableRequest(MatchQueuePointer.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        dynamoDBClient.createTable(createTableRequest);

        return "Success";

    }

    @POST
    @Path("/uninstall")
    @Produces("text/plain")
    public String uninstallApp(@HeaderParam("Access-Token") String accessToken, @Context final HttpServletResponse response) {

        long userId = authService.getUserId(accessToken);
        if (userId != 533081609) {
            throw new AuthenticationException("Not allowed");
        }

        String tablePrefix =config.getString("dynamodb.tablePrefix");
        if (tablePrefix == null || tablePrefix.equals("") || tablePrefix.equals("prod")) {
            throw new AuthenticationException("Not allowed in this environment: " + tablePrefix);
        }

        logger.info("Deleting All tables");

        CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(User.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Friend.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Video.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(CheckIn.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Like.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(Pass.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        createTableRequest = mapper.generateCreateTableRequest(MatchQueuePointer.class);
        dynamoDBClient.deleteTable(createTableRequest.getTableName());

        return "Success";
    }

    @POST
    @Path("/testUsers")
    @Produces("text/plain")
    public String createTestData(
            @HeaderParam("Access-Token") String accessToken,
            @FormParam("count") int count,
            @FormParam("videoUrl") String vidUrl,
            @FormParam("lat") Float latitude,
            @FormParam("long") Float longitude

    ) {
        long userId = authService.getUserId(accessToken);
        if (userId != 533081609) {
            throw new AuthenticationException("Not allowed");
        }

        String tablePrefix =config.getString("dynamodb.tablePrefix");
        if (tablePrefix == null || tablePrefix.equals("") || tablePrefix.equals("prod")) {
            throw new AuthenticationException("Not allowed in this environment: " + tablePrefix);
        }

        logger.info("Creating test users");

        Fairy fairy = Fairy.create(Locale.forLanguageTag("en"));
        Random random = new Random();
        List<User> users = new ArrayList<User>(count);
        for (int i=0; i < count; i++) {
            Person person = fairy.person(PersonProperties.minAge(18));
            User user = new User();
            if (vidUrl != null) {
                user.setVideoUri(vidUrl);
            }
            if (latitude != null && longitude != null) {
                user.setLocation(latitude, longitude);
            } else {
                user.setLocation((float) randomInRange(37, 42), (float) randomInRange(-114, -109));
            }
            user.setFirstName(person.firstName());
            user.setLastName(person.lastName());
            user.setGender(person.isMale() ? "M" : "F");
            user.setDateOfBirth(person.dateOfBirth().toDate());

            user.setPoints(random.nextInt(100));
            user.setUserId(Math.abs(random.nextLong()));
            user.setLastShown(new Date());
            users.add(user);
        }

        mapper.batchSave(users);
        return "Saved " + count + " Users";
    }

    private double randomInRange(float min, float max) {
        return Math.random() * (max-min) + min;
    }

    private void waitForTableToBecomeAvailable(String tableName) {
        logger.debug("Waiting for " + tableName + " to become ACTIVE...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            DescribeTableRequest request = new DescribeTableRequest()
                    .withTableName(tableName);
            TableDescription tableDescription = this.dynamoDBClient.describeTable(
                    request).getTable();
            String tableStatus = tableDescription.getTableStatus();
            System.out.println("  - current state: " + tableStatus);
            if (tableStatus.equals(TableStatus.ACTIVE.toString()))
                return;
            try { Thread.sleep(1000 * 20); } catch (Exception e) { }
        }
        throw new RuntimeException("Table " + tableName + " never went active");
    }
}
