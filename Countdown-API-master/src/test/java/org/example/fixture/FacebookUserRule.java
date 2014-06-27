package org.example.fixture;

import com.amazonaws.services.ec2.AmazonEC2;
import com.google.inject.Provider;
import com.rurri.countdown.model.User;
import com.typesafe.config.Config;
import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.RawAPIResponse;
import facebook4j.TestUser;
import facebook4j.auth.AccessToken;
import facebook4j.conf.ConfigurationBuilder;
import org.jfairy.Fairy;
import org.jfairy.producer.person.Person;
import org.jfairy.producer.person.PersonProperties;
import org.junit.rules.ExternalResource;

import javax.inject.Inject;
import java.util.*;

public class FacebookUserRule extends ExternalResource {

    private String accessToken;
    private String id;
    private Facebook fb;


    private String appId = "255561074614999";

    public FacebookUserRule() {

        Facebook facebook = new FacebookFactory().getInstance();

        facebook.setOAuthAppId(appId, "255561074614999");
        facebook.setOAuthAccessToken(new AccessToken("255561074614999|6n7GmyO00GSohdBM8CF5ypRIPPQ", null));
        this.fb = facebook;

    }

    public void start() throws Exception {
        Map<String, String> params = new HashMap<String, String>();

        RawAPIResponse response = this.fb.rawAPI().callPostAPI(this.appId + "/accounts/test-users");
        this.accessToken = response.asJSONObject().getString("access_token");
        this.id = response.asJSONObject().getString("id");
    }

    public void stop() throws Exception {
        this.fb.deleteTestUser(this.id);

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

    public String getId() {
        return id;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
