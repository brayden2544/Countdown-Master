package com.rurri.countdown.resource;

/**
 * Created by jason on 5/6/14.
 */

import com.google.inject.Inject;
import com.rurri.countdown.exception.AuthenticationException;
import com.rurri.countdown.model.Like;
import com.rurri.countdown.model.Pass;
import com.rurri.countdown.repository.UserRepository;
import com.rurri.countdown.exception.InputValidationException;
import com.rurri.countdown.model.User;
import com.rurri.countdown.service.*;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.*;;import java.util.Collection;
import java.util.Date;
import java.util.List;

@Path("/user")
public class UserResource extends BaseResource {

    @Inject
    AuthenticationService authService;

    @Inject
    UserService userService;

    @Inject
    VideoService videoService;

    @Inject
    UserRepository userRepository;

    @Inject
    CheckInService checkInService;

    @Inject
    ConnectionService connectionService;

    @Inject
    CacheService cacheService;

    @Inject
    MatchService matchService;

    @POST
    @Produces("application/json")
    public User saveUser(
            @HeaderParam("Access-Token") String accessToken,
            @FormParam("lat") Float latitude,
            @FormParam("long") Float longitude,
            @FormParam("twitter_username") String twitterUsername
    ) {

        logger.debug("Page Request started for user POST");

        if (accessToken == null || accessToken.length() == 0) {
            logger.warn("No access token in Header of Request. Returning Invalid");
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        long userId = authService.getUserId(accessToken);
        org.slf4j.MDC.put("user_id", Long.toString(userId));

        User user = userRepository.getUser(userId);
        if (user == null) {
            logger.debug("No existing user found. Attempting to create.");
            try {
                user = userService.createUser(accessToken);
            } catch (Exception e) {
                logger.error("Problem creating user. Removing user from cache.", e);
                cacheService.removeAccessToken(accessToken);
                throw new RuntimeException(e);
            }
        }

        if (latitude != null && longitude != null) {
            logger.debug("lat/long information found. Calling CheckIn");
            checkInService.checkIn(user, latitude, longitude);
        }

        if (twitterUsername != null && !twitterUsername.equals(user.getTwitterUsername())) {
            user.setTwitterUsername(twitterUsername);
            userRepository.saveUser(user);
        }

        if (StringUtils.isNotBlank(user.getVideoUri())) {
            //Expire in 7 days for user's own video
            Date expiration = new Date(System.currentTimeMillis() + 604800000);
            user.setVideoUri(videoService.getPublicUrl(user, expiration).toExternalForm());
        }
        return user;
    }

    @POST
    @Path("/{id}/checkin")
    @Produces("application/json")
    public User checkIn(
            @HeaderParam("Access-Token") String accessToken,
            @FormParam("lat") Float latitude,
            @FormParam("long") Float longitude
    ) {

        if (accessToken == null || accessToken.length() == 0) {
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        if (latitude == null || longitude == null) {
            throw new InputValidationException("Request Invalid, POST params latitude and longitude are required");
        }

        long userId = authService.getUserId(accessToken);
        User user = userRepository.getUser(userId);
        checkInService.checkIn(user, latitude, longitude);

        return user;
    }

    @POST
    @Path("/{id}/pass")
    @Produces("application/json")
    public Pass passOnUser(
            @HeaderParam("Access-Token") String accessToken,
            @PathParam("id") long userIdFromPath)
    {

        if (accessToken == null || accessToken.length() == 0) {
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        if (userIdFromPath < 1) {
            throw new InputValidationException("Request Invalid, userIdInvalid");
        }

        long currentUserId = authService.getUserId(accessToken);
        Pass pass = connectionService.userPasses(currentUserId, userIdFromPath);
        return pass;
    }

    @POST
    @Path("/{id}/like")
    @Produces("application/json")
    public Like likeUser(
            @HeaderParam("Access-Token") String accessToken,
            @PathParam("id") long userIdFromPath)
    {

        if (accessToken == null || accessToken.length() == 0) {
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        if (userIdFromPath < 1) {
            throw new InputValidationException("Request Invalid, userIdInvalid");
        }

        long currentUserId = authService.getUserId(accessToken);
        Like like = connectionService.userLikes(currentUserId, userIdFromPath);
        return like;
    }

    @POST
    @Path("/{id}/nextPotentials")
    @Produces("application/json")
    public User[] nextPotentials(
            @HeaderParam("Access-Token") String accessToken,
            @PathParam("id") long userIdFromPath,
            @FormParam("count") Integer count,
            @FormParam("offset") Integer offset
            )
    {

        if (accessToken == null || accessToken.length() == 0) {
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        if (userIdFromPath < 1) {
            throw new InputValidationException("Request Invalid, userIdInvalid");
        }

        long currentUserId = authService.getUserId(accessToken);
        User currentUser = userRepository.getUser(currentUserId);

        if (count == null) {
            count = 5;
        }

        if (count > 20) {
            throw new InputValidationException("Cannot retrieve more than 20 potential matches");
        }

        if (offset == null) {
            offset = 0;
        }

        int totalRetrievalCount = count + offset;

        Collection<User> users = matchService.getNextMatches(currentUser, totalRetrievalCount);

        User userArray[] = new User[count];
        //Expire in 20 minutes for other user's videos
        Date expiration = new Date(System.currentTimeMillis() + 1200000);
        int i=0;
        int offsetCount = 0;
        for (User user : users) {
            if (offset > offsetCount++) {
                continue;
            }

            if (StringUtils.isNotBlank(user.getVideoUri())) {
                user.setVideoUri(videoService.getPublicUrl(user, expiration).toExternalForm());
            }
            userArray[i++] = user;
        }
        return userArray;
    }


}
