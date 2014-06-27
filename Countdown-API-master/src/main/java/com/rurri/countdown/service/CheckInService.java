package com.rurri.countdown.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.rurri.countdown.model.CheckIn;
import com.rurri.countdown.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class CheckInService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    Provider<DynamoDBMapper> mapperProvider;

    @Inject
    CacheService cacheService;

    public void checkIn(User user, Float latitude, Float longitude)  {
        if (user.getLongitude() == null || user.getLatitude() == null || !user.getLatitude().equals(latitude) || !user.getLongitude().equals(longitude)) {
            logger.debug("About to check in user to location: " + latitude + " , " + longitude);
            user.setLocation(latitude, longitude);

            CheckIn checkIn = new CheckIn();
            checkIn.setUserId(user.getUserId());
            checkIn.setGeoHash(user.getGeoHash());
            mapperProvider.get().batchSave(user, checkIn);
            cacheService.cacheUser(user);
            logger.info("CheckIn information saved.");
        } else {
            logger.info("CheckIn information unchanged from existing location. Doing nothing");
        }

    }
}