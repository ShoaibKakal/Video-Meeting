package com.shoaib.videomeeting.listeners;

import com.shoaib.videomeeting.models.User;

public interface UsersListener {

    //To start video meeting
    void initiateVideoMeeting(User user);

    //To start audio meeting
    void initiateAudioMeeting(User user);

    //To start Multiple User Action
    void onMultipleUsersAction(Boolean isMultipleUserAction);
}
