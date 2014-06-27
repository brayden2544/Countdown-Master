package com.rurri.countdown.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides @Singleton
    public Config provideRedis() {
        String environment = System.getProperty("APPLICATION_ENV");
        if (environment == null) {
            environment = System.getProperty("PARAM1");
        }
        if (environment == null) {
            environment = System.getenv("APPLICATION_ENV");
        }
        if (environment == null) {
            environment = System.getenv("PARAM1");
        }
        Config config = ConfigFactory.load();
        if (environment != null) {
            return ConfigFactory.load(environment).withFallback(config);
        } else {
            return config;
        }
    }
}
