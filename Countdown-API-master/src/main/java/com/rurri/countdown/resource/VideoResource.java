package com.rurri.countdown.resource;

/**
 * Created by jason on 5/6/14.
 */

import com.google.inject.Inject;
import com.rurri.countdown.exception.AuthenticationException;
import com.rurri.countdown.repository.UserRepository;
import com.rurri.countdown.exception.InputValidationException;
import com.rurri.countdown.model.User;
import com.rurri.countdown.service.AuthenticationService;
import com.rurri.countdown.service.CheckInService;
import com.rurri.countdown.service.UserService;
import com.rurri.countdown.service.VideoService;

import javax.ws.rs.*;

;import java.io.FileOutputStream;
import java.io.IOException;

@Path("/user/{id}/video")
public class VideoResource extends BaseResource {

    @Inject
    AuthenticationService authService;

    @Inject
    VideoService videoService;

    @Inject
    UserRepository userRepository;

    @POST
    @Produces("application/json")
    public User saveVideo(
            @HeaderParam("Access-Token") String accessToken,
            @HeaderParam("Content-Length") long contentLength,
            @HeaderParam("Content-Type") String contentType,
            @PathParam("id") long userIdFromPath,
            byte data[]
    ) {

        logger.debug("Page Request started for video POST");

        if (accessToken == null || accessToken.length() == 0) {
            logger.warn("No access token in Header of Request. Returning Invalid");
            throw new InputValidationException("Request header 'Access-Token' is required.");
        }

        if (contentLength == 0) {
            logger.warn("No Content-Length in Header of Request. Returning Invalid");
            throw new InputValidationException("Request header 'Content-Length' is required.");
        }

        if (contentLength < 1000) {
            logger.warn("Content-Length is too small. There is no way this could be a video file at less than 1k of data.");
            throw new InputValidationException("Content-Length is too small. There is no way this could be a video file at less than 1k of data.");
        }

        if (contentType == null || contentType.length() == 0 || !contentType.equals("video/quicktime")) {
            logger.warn("Request header 'Content-Type' is required, and only data of type video/quicktime is supported");
            throw new InputValidationException("Request header 'Content-Type' is required, and only data of type video/quicktime is supported");
        }

        long userId = authService.getUserId(accessToken, userIdFromPath);
        org.slf4j.MDC.put("user_id", Long.toString(userId));
        User user = userRepository.getUser(userId);
        if (user == null) {
            throw new AuthenticationException("User does not exist");
        }

        if (data == null || data.length == 0) {
            logger.warn("No binary data in Request Body");
            throw new InputValidationException("Video contents required as request body");
        }

        videoService.storeVideo(user, data, contentType);
        logger.info("Video Finished Storing. Returning user contents");

        return user;
    }
}
