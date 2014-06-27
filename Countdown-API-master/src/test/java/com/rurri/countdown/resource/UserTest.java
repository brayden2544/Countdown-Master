package com.rurri.countdown.resource;

import com.google.inject.Inject;
import com.jayway.restassured.RestAssured;
import com.rurri.countdown.di.BaseAWSModule;
import com.rurri.countdown.di.ConfigModule;
import com.rurri.countdown.di.FacebookModule;
import com.typesafe.config.Config;
import facebook4j.Facebook;
import org.example.fixture.ContainerRule;
import org.example.fixture.DynamoDBLocalRule;
import org.example.fixture.FacebookUserRule;
import org.example.fixture.ServerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;

public class UserTest {

    @Rule
    public ServerRule serverRule = new ServerRule(7999);

    @Rule
    public DynamoDBLocalRule dynamoDBLocalRule = new DynamoDBLocalRule(8000);

    @Rule
    public FacebookUserRule facebookUser = new FacebookUserRule();

    @Before
    public void before() {
        RestAssured.baseURI = "http://127.0.0.1:7999";
    }

    @Test
    public void userGetNotAllowed() {
        expect().
                statusCode(405).
                when().get("/user");
    }

    @Test
    public void userPostNoParams() {
        expect().
                statusCode(400).
                when().
                post("/user");
    }

    @Test
    public void userPostNoToken() {
        expect().
                statusCode(401).
                when().request().header("Access-Token", "Does not exist").
                post("/user");
    }

    @Test
    public void testUserCreate() {
        expect().
                statusCode(200).
                when().request().header("Access-Token", this.facebookUser.getAccessToken())
                .post("/user");
    }



}
