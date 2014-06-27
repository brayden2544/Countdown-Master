package com.rurri.countdown.di;

import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.google.inject.*;
import com.typesafe.config.Config;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountdownAWSModule extends AbstractModule {


    @Override
    protected void configure() {
    }

    @Provides
    public DynamoDBMapper provideMapper(Config config, AmazonDynamoDB dynamoDBClient) {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(
                DynamoDBMapperConfig.SaveBehavior.UPDATE,
                DynamoDBMapperConfig.ConsistentReads.EVENTUAL,
                DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix(config.getString("dynamodb.tablePrefix")),
                DynamoDBMapperConfig.PaginationLoadingStrategy.ITERATION_ONLY,
                RequestMetricCollector.NONE);

        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient, mapperConfig);
        return mapper;
    }

    @Provides
    public ExecutorService provideExecutorService(Config config) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        return executor;
    }
}
