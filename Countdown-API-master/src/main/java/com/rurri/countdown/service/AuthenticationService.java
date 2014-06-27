package com.rurri.countdown.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.rurri.countdown.exception.AuthenticationException;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.auth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    Provider<Facebook> fbProvider;


    @Inject
    CacheService cacheService;

    public long getUserId(String accessToken) throws AuthenticationException {
        Long cachedUserId = cacheService.getUserIdByToken(accessToken);
        if (cachedUserId != null) {
            return cachedUserId;
        }

        Facebook facebook = fbProvider.get();
        facebook.setOAuthAccessToken(new AccessToken(accessToken));

        try {
            facebook4j.User fbUser = facebook.getUser("me");
            if (fbUser != null) {
                long userId = Long.valueOf(fbUser.getId());
                cacheService.cacheUserToken(accessToken, userId);
                return userId;
            } else {
                logger.info("Unable to authenticate with facebook. Facebook returned null.");
                throw new AuthenticationException("Unable to authenticate with facebook. ");
            }
        } catch (FacebookException e) {
            logger.info("Unable to authenticate with facebook. Facebook threw an exception.", e);
            throw new AuthenticationException("Unable to authenticate with facebook. " + e.getMessage());
        }
    }

    public long getUserId(String accessToken, long userIdFromPath) throws AuthenticationException {
        long userId = this.getUserId(accessToken);
        if (userId != userIdFromPath) {
            logger.info("User retrieved from auth, doesn't match expected user. Expected:" + userIdFromPath + " Retrieved:" + userId);
            throw new AuthenticationException("Unable to authenticate with facebook. ");
        }
        return userId;
    }
}