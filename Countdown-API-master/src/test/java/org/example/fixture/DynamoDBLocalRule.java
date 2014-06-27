package org.example.fixture;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.rurri.countdown.model.*;
import org.junit.rules.ExternalResource;

public class DynamoDBLocalRule extends ExternalResource {
	private int port;

	private final String jarPath;

    private Process proc;

	public DynamoDBLocalRule() {
		this(8000);
	}

	public DynamoDBLocalRule(int port) {
		this(port, "src/test/dynamodb_local");
	}

	public DynamoDBLocalRule(int port, String jarPath) {
		this.port = port;
		this.jarPath = jarPath;
	}

	public void start() throws Exception {

        System.setProperty("aws.accessKeyId", "AKIAJGZARNVMSDTNBCJQ");
        System.setProperty("aws.secretKey", "NONE");

        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient();
        dynamoDBClient.setEndpoint("http://localhost:8000");

        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.UPDATE,
                DynamoDBMapperConfig.ConsistentReads.EVENTUAL,
                DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix("unit_test"),
                DynamoDBMapperConfig.PaginationLoadingStrategy.ITERATION_ONLY,
                RequestMetricCollector.NONE);

        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient, mapperConfig);

        CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(User.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(User.getIndexes());
        dynamoDBClient.createTable(createTableRequest);


        createTableRequest = mapper.generateCreateTableRequest(Like.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(Like.getIndexes());
        dynamoDBClient.createTable(createTableRequest);

        createTableRequest = mapper.generateCreateTableRequest(Pass.class);
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        createTableRequest.setGlobalSecondaryIndexes(Pass.getIndexes());
        dynamoDBClient.createTable(createTableRequest);

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

	}

	public void stop() throws Exception {

        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient();
        dynamoDBClient.setEndpoint("http://localhost:8000");

        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.UPDATE,
                DynamoDBMapperConfig.ConsistentReads.EVENTUAL,
                DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix("unit_test"),
                DynamoDBMapperConfig.PaginationLoadingStrategy.ITERATION_ONLY,
                RequestMetricCollector.NONE);

        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient, mapperConfig);

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
	}

	@Override
	protected void before() throws Throwable {
		start();
	}

	@Override
	protected void after() {
		try {
			stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}