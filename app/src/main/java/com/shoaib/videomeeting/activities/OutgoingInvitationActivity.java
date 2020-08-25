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

import com.facebook.react.modules.toast.ToastModule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shoaib.videomeeting.R;
import com.shoaib.videomeeting.models.User;
import com.shoaib.videomeeting.network.ApiClient;
import com.shoaib.videomeeting.network.ApiService;
import com.shoaib.videomeeting.utilities.Constants;
import com.shoaib.videomeeting.utilities.PreferenceManager;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private static final String TAG = "OutgoingInvitation";
    private ImageView imageMeetingType, imageStopInvitation;
    private TextView textFirstChar, textUsername, textUserEmail ;

    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    private String meetingRoom = null;
    private String meetingType = null;

    private int rejectionCount = 0;
    private int totalReceivers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_invitation);
        imageMeetingType = findViewById(R.id.imageMeetingType);
        imageStopInvitation = findViewById(R.id.imageStopInvitation);
        textFirstChar = findViewById(R.id.textFirstChar);
        textUserEmail = findViewById(R.id.textEmail);
        textUsername = findViewById(R.id.textUsername);

        preferenceManager = new PreferenceManager(getApplicationContext());




        meetingType = getIntent().getStringExtra("type");

        if (meetingType != null ){
            if (meetingType.equals("video")){
                imageMeetingType.setImageResource(R.drawable.ic_video);
            }
            else if (meetingType.equals("audio")){
                imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }



        User user = (User) getIntent().getSerializableExtra("user");
        if (user != null){
            textUsername.setText(String.format("%s %s",user.firstName, user.lastName));
            textFirstChar.setText(user.firstName.substring(0,1));
            textUserEmail.setText(user.email);
        }

        imageStopInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent().getBooleanExtra("isMultiple", false)) {
                    Type type = new TypeToken<ArrayList<User>>(){}.getType();
                    ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                    cancelInvitation(null, receivers);
                }
               else {
                    if (user != null){
                        cancelInvitation(user.token, null);
                    }
                }

            }
        });

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful() && task.getResult() != null){
                    inviterToken = task.getResult().getToken();

                    if (meetingType != null){
                        if (getIntent().getBooleanExtra("isMultiple", false)){
                            Type type = new TypeToken<ArrayList<User>>(){}.getType();
                            ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                            if (receivers != null){
                                totalReceivers = receivers.size();
                            }
                            initiateMeeting(meetingType, null, receivers);
                        }
                        else{
                            if ( user != null){
                                totalReceivers = 1;
                                initiateMeeting(meetingType, user.token, null);
                            }
                        }
                    }

                }
            }
        });


    }//onCreate ends

    public void cancelInvitation( String receiverToken, ArrayList<User> receivers){
        try {

            JSONArray tokens = new JSONArray();
            if (receiverToken != null){
                tokens.put(receiverToken);
            }

            if (receivers != null && receivers.size()>0){
                for (User user : receivers){
                    tokens.put(user.token);
                }
            }



            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    public void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers){

        try {
            JSONArray tokens = new JSONArray();
            if (receiverToken !=null){
                tokens.put(receiverToken);
            }

            if (receivers != null && receivers.size() > 0){
                StringBuilder userNames = new StringBuilder();
                for (int i=0; i< receivers.size(); i++){
                    tokens.put(receivers.get(i).token);
                    userNames.append(receivers.get(i).firstName).append(" ").append(receivers.get(i).lastName).append("\n");
                }
                textFirstChar.setVisibility(View.GONE);
                textUserEmail.setVisibility(View.GONE);
                textUsername.setText(userNames.toString());
            }

            JSONObject data = new JSONObject();
            JSONObject body = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION); //REMOTE_MSG_TYPE = "invitation";
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));

            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);
            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID) + "_" + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);


        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }


    }

        public void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody) // remoteMessageBody = {"data":{inviter data},"tokens":[receiver tokens]}
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                        if (response.isSuccessful()){
                            if (type.equals(Constants.REMOTE_MSG_INVITATION)){
                                Toast.makeText(OutgoingInvitationActivity.this, "Invitation sent successful", Toast.LENGTH_SHORT).show();
                            }
                            else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                                Toast.makeText(OutgoingInvitationActivity.this, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }else{
                            Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }
                    @Override
                    public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {
                        Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    public BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);

            if (type != null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
//                    Toast.makeText(context, "Invitation Accepted", Toast.LENGTH_SHORT).show();
                    try {
                        User user = (User) getIntent().getSerializableExtra("user");
                        assert user != null;
//                        URL serverURL = new URL("https://meet.jit.si/roomname#userInfo.displayName="+user.firstName);
                        URL serverURL = new URL("https://meet.jit.si");

                        JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                        builder.setServerURL(serverURL);
                        builder.setWelcomePageEnabled(false);
                        builder.setRoom(meetingRoom);
                        if (meetingType.equals("audio")) {
                            builder.setVideoMuted(true);
                        }

                        JitsiMeetActivity.launch(OutgoingInvitationActivity.this,builder.build());
                        finish();
                    }catch (Exception e){
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                    rejectionCount+=1;
                    if (rejectionCount==totalReceivers){
                        Toast.makeText(context, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }

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