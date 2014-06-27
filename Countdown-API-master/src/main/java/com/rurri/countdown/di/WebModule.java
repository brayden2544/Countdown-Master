package com.rurri.countdown.di;

import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.rurri.countdown.resource.InstallResource;
import com.rurri.countdown.resource.VideoResource;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import com.rurri.countdown.resource.UserResource;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebModule extends ServletModule {
	@Override
	protected void configureServlets() {
        bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);
		bind(GuiceContainer.class);
		serve("/*").with(GuiceContainer.class);



        bind(FacebookModule.class);
        bind(UserResource.class);
        bind(InstallResource.class);
        bind(VideoResource.class);
	}
}
