package com.rurri.countdown.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.rurri.countdown.model.Like;
import com.rurri.countdown.model.Pass;
import com.rurri.countdown.repository.UserRepository;
import com.rurri.countdown.exception.AuthenticationException;
import com.rurri.countdown.model.Friend;
import com.rurri.countdown.model.User;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    UserRepository userRepository;

    @Inject
    Provider<Facebook> fbProvider;

    @Inject
    Provider<DynamoDBMapper> mapperProvider;

    public User createUser(String accessToken) throws AuthenticationException {
        Facebook facebook = fbProvider.get();
        facebook.setOAuthAccessToken(new AccessToken(accessToken));

        try {
            facebook4j.User fbUser = facebook.getUser("me");

            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

            User user = new User();
            if (fbUser.getBirthday() != null) {
                try {
                    user.setDateOfBirth(format.parse(fbUser.getBirthday()));
                } catch (ParseException e) {
                    logger.error("Error parsing facebook date", e);
                    throw new RuntimeException("Unable to parse Facebook Date");
                }
            }

            user.setUserId(Long.parseLong(fbUser.getId()));
            user.setFirstName(fbUser.getFirstName());
            user.setLastName(fbUser.getLastName());

            if (fbUser.getGender().equals("male")) {
                user.setGender("M");
            } else if (user.getGender().equals("female")) {
                user.setGender("F");
            } else {
                logger.error("Unknown Gender, expected male/female");
                //@TODO Handle other gender types here after searching for them is figured out.
                throw new AuthenticationException("Unable to match Gener. Gender is required");
            }

            user.setTimeZone(Double.toString(fbUser.getTimezone()));
            user.setLocale(fbUser.getLocale().toString());

            user.setPoints(0);
            user.setGenderPoints(null);

            userRepository.saveUser(user);
            logger.debug("User Saved to DB. Looking up friends information");

            ResponseList<facebook4j.Friend > fbFriends = facebook.friends().getFriends();
            List<Friend> friends = new ArrayList<Friend>(fbFriends.size());
            for(facebook4j.Friend  fbFriend: fbFriends) {
                Friend friend = new Friend();
                friend.setUserId(user.getUserId());
                friend.setFriendId(Long.parseLong(fbFriend.getId()));
                friends.add(friend);
            }
            List<DynamoDBMapper.FailedBatch> failures = mapperProvider.get().batchSave(friends);
            if (failures.size() > 0) {
                String errorString = "Unexpected error saving friends to database.\n";
                for (DynamoDBMapper.FailedBatch failedBatch: failures) {
                    if (failedBatch.getException() != null) {
                        errorString += failedBatch.getException().getMessage();
                    }
                }
                logger.error(errorString);
                throw new RuntimeException("Unable to Write friends to DB");
            }
            logger.info("User and friends information saved to DB.");
            return user;
        } catch (FacebookException e) {
            logger.error("Unexpected error from facebook.", e);
            throw new RuntimeException(e);
        }
    }

    public User calculatePoints(User user) {
        DynamoDBMapper mapper = mapperProvider.get();
        Like like = new Like();
        like.setLikedUserId(user.getUserId());
        DynamoDBQueryExpression<Like> queryExpressionLikes = new DynamoDBQueryExpression<Like>().withIndexName("Liked").withHashKeyValues(like);
        long likes = mapper.count(Like.class, queryExpressionLikes);

        Pass pass = new Pass();
        pass.setPassedUserId(user.getUserId());
        DynamoDBQueryExpression<Pass> queryExpressionPasses = new DynamoDBQueryExpression<Pass>().withIndexName("Passed").withHashKeyValues(pass);
        long passes = mapper.count(Pass.class, queryExpressionPasses);

        int score = 0;
        if (passes > 0 || likes > 0) {
            int ratioPercent = (int) ((likes * 100) / (passes + likes)) / 100;
            score = (int) Math.round((Math.max(likes, 100) * .5) + (ratioPercent * .5));
        }

        if (user.getPoints() != score) {
            user.setPoints(score);
            userRepository.saveUser(user);
            logger.info("New point value saved for user: " + user.getUserId() + " points: " + score + " g-pts:" + user.getGenderPoints());
        }
        return user;
    }




}