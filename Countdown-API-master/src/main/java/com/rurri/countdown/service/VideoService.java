package com.rurri.countdown.service;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.rurri.countdown.model.User;
import com.rurri.countdown.model.Video;
import com.rurri.countdown.repository.UserRepository;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

public class VideoService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Inject
    private Config config;

    @Inject
    UserRepository userRepository;

    @Inject
    UserService userService;

    @Inject
    Provider<DynamoDBMapper> mapperProvider;

    @Inject
    Provider<AmazonS3> amazonS3Provider;

    Provider<AmazonCloudFront> amazonCloudFrontProvider;

    public User storeVideo(User user, byte[] contents, String contentType) {
        String bucketName = config.getString("s3.videoBucket");
        AmazonS3 amazonS3 = amazonS3Provider.get();
        String fileName = UUID.randomUUID() + ".mov";
        ObjectMetadata metadata = new ObjectMetadata();
        Map<String, String> userData = new HashMap<String, String>();
        userData.put("user_id", Long.toString(user.getUserId()));
        metadata.setContentType(contentType);
        metadata.setContentLength(contents.length);
        metadata.setUserMetadata(userData);


        Video video = new Video();
        video.setUserId(user.getUserId());
        video.setFileName(fileName);
        video.setCreated(new Date());
        video.setSize(contents.length);

        mapperProvider.get().save(video);
        logger.debug("Video information saved in db, uploading to S3.");


        amazonS3.putObject(bucketName, fileName, new ByteArrayInputStream(contents), metadata);

        logger.debug("Video uploaded to S3");
        user.setVideoUri(fileName);
        userRepository.saveUser(user);
        if (user.getPoints() == null) {
            user = userService.calculatePoints(user);
        }
        logger.debug("User information updated with video");
        return user;
    }

    public URL getPublicUrl(User user, Date expiration) {
        String bucketName = config.getString("s3.videoBucket");
        AmazonS3 amazonS3 = amazonS3Provider.get();
        return amazonS3.generatePresignedUrl(bucketName, user.getVideoUri(), expiration);
    }
}