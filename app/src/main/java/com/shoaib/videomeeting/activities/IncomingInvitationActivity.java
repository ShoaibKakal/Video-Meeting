package com.shoaib.videomeeting.activities;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.shoaib.videomeeting.R;
import com.shoaib.videomeeting.network.ApiClient;
import com.shoaib.videomeeting.network.ApiService;
import com.shoaib.videomeeting.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    private static final String TAG = "IncomingInvitation";
    private ImageView imageMeetingType, imageAcceptInvitation, imageRejectInvitation;
    private TextView textFirstChar, textInitiatorUsername, textEmail;
    private String meetingType = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invitation);
        imageMeetingType = findViewById(R.id.imageMeetingType);
        imageAcceptInvitation = findViewById(R.id.imageAcceptInvitation);
        imageRejectInvitation = findViewById(R.id.imageRejectInvitation);
        textFirstChar = findViewById(R.id.textFirstChar);
        textInitiatorUsername = findViewById(R.id.textInitiatorUsername);
        textEmail = findViewById(R.id.textInitiatorEmail);

        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);

        if (meetingType != null){
            if (meetingType.equals("video")){
                imageMeetingType.setImageResource(R.drawable.ic_video);
            }

            else if (meetingType.equals("audio")){
                imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }

        textInitiatorUsername.setText(String.format("%s %s",getIntent().getStringExtra(Constants.KEY_FIRST_NAME), getIntent().getStringExtra(Constants.KEY_LAST_NAME)));
        textFirstChar.setText(Objects.requireNonNull(getIntent().getStringExtra(Constants.KEY_FIRST_NAME)).substring(0,1));
        textEmail.setText(getIntent().getStringExtra(Constants.KEY_EMAIL));


        imageAcceptInvitation.setOnClickListener(v -> sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_ACCEPTED, getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)));

        imageRejectInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_REJECTED , getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN));
            }
        });

    }  //onCreate ends

    public void sendInvitationResponse(String response, String inviterToken){
        try {

            JSONArray tokens = new JSONArray();
            tokens.put(inviterToken);


            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, response);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), response);
            Log.d(TAG, "sendInvitationResponse: " + response);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()){
                            if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
//                                Toast.makeText(IncomingInvitationActivity.this, "Invitation Accepted", Toast.LENGTH_SHORT).show();
                                try {
                                    URL serverURL = new URL("https://meet.jit.si/roomname#userInfo.displayName="+Constants.KEY_FIRST_NAME);

                                    JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                                    builder.setServerURL(serverURL);
                                    builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                                    builder.setWelcomePageEnabled(true);

                                    if (meetingType.equals("audio")){
                                        builder.setVideoMuted(true);
                                    }

                                    JitsiMeetActivity.launch(IncomingInvitationActivity.this, builder.build());
                                    finish();
                                }catch (Exception e){
                                    Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                            else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                                Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                                finish();

                            }
                        }else{
                            Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
//                            Log.d(TAG, "onResponse: "+ response.message());
                            finish();

                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                        Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }


    public BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);

            if (type != null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                    Toast.makeText(context, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }

            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver, new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

}