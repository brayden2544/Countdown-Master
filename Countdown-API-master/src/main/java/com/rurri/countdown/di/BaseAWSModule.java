package com.rurri.countdown.di;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkAsync;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsync;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceAsync;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsync;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsync;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.binder.ScopedBindingBuilder;
import com.typesafe.config.Config;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This small behemoth is likely to be moved into cloudy <a
 * href="https://bitbucket.org/ingenieux/cloudy"
 * >https://bitbucket.org/ingenieux/cloudy</a>, as it contains several
 * interesting features.
 * 
 * @author aldrin
 * 
 */
public class BaseAWSModule extends AbstractModule implements Module {
	private AWSCredentialsProviderChain providerChain = new BeanstalkerCredentialsProviderChain();

	private ClientConfiguration clientConfiguration = new ClientConfiguration();

	private ExecutorService executorService = null;

    Config config;

	/**
	 * Represents a Guice Provider Factory for General Instantiation of AWS
	 * Clients
	 * 
	 * @author aldrin
	 * 
	 * @param <K>
	 *            the Service Interface class of an AWS Service Client
	 */
	public class AWSClientProvider<K> implements Provider<K> {
		private Constructor<K> ctor;



		@SuppressWarnings("unchecked")
		public AWSClientProvider(Class<K> serviceClazz)
				throws NoSuchMethodException, SecurityException,
				ClassNotFoundException {
			boolean asyncP = serviceClazz.getSimpleName().endsWith("Async");


			Class<K> clientClazz = (Class<K>) Class.forName(serviceClazz
					.getName() + "Client");

			this.ctor = asyncP ? clientClazz.getConstructor(
					AWSCredentials.class, ClientConfiguration.class,
					ExecutorService.class) : clientClazz.getConstructor(
					AWSCredentials.class, ClientConfiguration.class);
		}

		@Override
		public K get() {
			AWSCredentials awsCreds = getCredentials();
			ClientConfiguration clientConfig = getClientConfiguration();
            Config config = BaseAWSModule.this.config;
			try {
				if (2 == ctor.getParameterTypes().length) {
                    K client = ctor.newInstance(awsCreds, clientConfig);
                    if (client instanceof AmazonDynamoDBClient && config.hasPath("aws.endpoint")) {
                        ((AmazonWebServiceClient)client).setEndpoint(config.getString("aws.endpoint"));
                    }
                    return client;
				}

				ExecutorService executorService = getExecutorService();

                if (executorService == null) {
                    //@TODO IMPORTANT. SETUP EXECUTOR SERVICE CORRECTLY
                    executorService = Executors.newCachedThreadPool();
                }
                K client = ctor.newInstance(awsCreds, clientConfig, executorService);
                if (client instanceof AmazonDynamoDBClient && config.hasPath("aws.endpoint")) {
                    ((AmazonWebServiceClient)client).setEndpoint(config.getString("aws.endpoint"));
                }
                return client;
			} catch (Exception exc) {
				throw new RuntimeException(exc);
			}
		}
	}

	public AWSCredentials getCredentials() {
		return getProviderChain().getCredentials();
	}

	private AWSCredentialsProviderChain getProviderChain() {
		return providerChain;
	}

	public ClientConfiguration getClientConfiguration() {
		return clientConfiguration;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setClientConfiguration(ClientConfiguration clientConfiguration) {
		this.clientConfiguration = clientConfiguration;
	}

	public void setProviderChain(AWSCredentialsProviderChain providerChain) {
		this.providerChain = providerChain;
	}

	public <K> ScopedBindingBuilder bindClient(Class<K> clientClazz) {
		try {
			AWSClientProvider<K> provider = new AWSClientProvider<K>(
					clientClazz);

			return bind(clientClazz).toProvider(provider);
		} catch (Exception e) {
			if (RuntimeException.class.isAssignableFrom(e.getClass()))
				throw (RuntimeException) e;

			throw new RuntimeException(e);
		}
	}

	public void bindClients(Class<?>... serviceClasses) {
		for (Class<?> serviceClass : serviceClasses)
			bindClient(serviceClass);
	}

	@Override
	protected void configure() {
		configureClients();
	}

	protected void configureClients() {
		Class<?>[] serviceClasses = getServiceClasses();

		if (null == serviceClasses) {
			serviceClasses = new Class<?>[] { AmazonCloudFront.class, //
					AmazonCloudFrontAsync.class, //
					AmazonCloudWatch.class, //
					AmazonCloudWatchAsync.class, //
					AmazonDynamoDB.class, //
					AmazonDynamoDBAsync.class, //
					AmazonEC2.class, //
					AmazonEC2Async.class, //
					AWSElasticBeanstalk.class, //
					AWSElasticBeanstalkAsync.class, //
					AmazonElasticLoadBalancing.class, //
					AmazonElasticLoadBalancingAsync.class, //
					AmazonElasticMapReduce.class, //
					AmazonElasticMapReduceAsync.class, //
					AmazonIdentityManagement.class, //
					AmazonIdentityManagementAsync.class, //
					AmazonRDS.class, //
					AmazonRDSAsync.class, //
					AmazonRoute53.class, //
					AmazonRoute53Async.class, //
					AmazonS3.class, //
					AWSSecurityTokenService.class, //
					AWSSecurityTokenServiceAsync.class, //
					AmazonSimpleDB.class, //
					AmazonSimpleDBAsync.class, //
					AmazonSimpleEmailService.class, //
					AmazonSimpleEmailServiceAsync.class, //
					AmazonSimpleWorkflow.class, //
					AmazonSimpleWorkflowAsync.class, //
					AmazonSQS.class, //
					AmazonSQSAsync.class, //
					AmazonSNS.class, //
					AmazonSNSAsync.class
			};
		}

		bindClients(serviceClasses);
	}

	protected Class<?>[] getServiceClasses() {
		return null;
	}

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}
