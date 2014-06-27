package com.rurri.countdown.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import com.typesafe.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebContextListener extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        BaseAWSModule baseAWSModule = new BaseAWSModule();
        Injector injector =  Guice.createInjector(new ConfigModule(), baseAWSModule, new WebModule(), new RedisModule(), new FacebookModule(), new CountdownAWSModule());
        Config config = injector.getInstance(Config.class);
        baseAWSModule.setConfig(config);

        if (config.getBoolean("mocks.testing")) {
            Module testingModule = injector.getInstance(TestingModule.class);
            return injector.createChildInjector(testingModule);
        } else {
            return injector;
        }

    }
}
