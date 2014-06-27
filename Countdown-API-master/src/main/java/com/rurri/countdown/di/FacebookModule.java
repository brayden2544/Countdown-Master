package com.rurri.countdown.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.conf.ConfigurationBuilder;

public class FacebookModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    public Facebook provideFBClient( Config config) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false)
                .setOAuthAppId(config.getString("fb.appId"))
                .setOAuthAppSecret("fb.appSecret");

        FacebookFactory ff = new FacebookFactory(cb.build());
        return ff.getInstance();
    }


}
