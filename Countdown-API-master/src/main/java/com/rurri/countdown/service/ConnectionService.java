package com.rurri.countdown.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.rurri.countdown.exception.AuthenticationException;
import com.rurri.countdown.model.Friend;
import com.rurri.countdown.model.Like;
import com.rurri.countdown.model.Pass;
import com.rurri.countdown.model.User;
import com.rurri.countdown.repository.UserRepository;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    Provider<DynamoDBMapper> mapperProvider;

    @Inject
    CacheService cacheService;

    public Pass userPasses(long sourceUser, long targetUser) throws AuthenticationException {
        Pass pass = new Pass();
        pass.setUserId(sourceUser);
        pass.setPassedUserId(targetUser);
        pass.setPassedAt(new Date());

        mapperProvider.get().save(pass);
        cacheService.addPassToCache(pass);
        return pass;
    }

    public Like userLikes(long sourceUser, long targetUser) throws AuthenticationException {
        Like like = new Like();
        like.setUserId(sourceUser);
        like.setLikedUserId(targetUser);
        like.setLikedAt(new Date());
        like.setReciprocal(-1);

        mapperProvider.get().save(like);
        cacheService.addLikeToCache(like);
        return like;
    }
}