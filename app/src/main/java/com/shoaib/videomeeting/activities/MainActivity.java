package com.shoaib.videomeeting.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.shoaib.videomeeting.R;
import com.shoaib.videomeeting.adapters.UsersAdapter;
import com.shoaib.videomeeting.listeners.UsersListener;
import com.shoaib.videomeeting.models.User;
import com.shoaib.videomeeting.utilities.Constants;
import com.shoaib.videomeeting.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UsersListener {

    private static final String TAG = "mainActivity";
    private PreferenceManager preferenceManager;
    private TextView textTitle;
    private TextView signOut;
    private TextView errorMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Runnable refresh;
    private ImageView imageConference;

    private RecyclerView usersRecyclerView;
    private List<User> users = new ArrayList<>();
    private UsersAdapter usersAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textTitle = findViewById(R.id.textTitle);
        signOut = findViewById(R.id.textSignOut);
        usersRecyclerView = findViewById(R.id.userRecyclerView);
        errorMessage = findViewById(R.id.errorMessage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        imageConference = findViewById(R.id.imageConference);

        preferenceManager = new PreferenceManager(getApplicationContext());

        textTitle.setText(String.format("%s %s", preferenceManager.getString(Constants.KEY_FIRST_NAME), preferenceManager.getString(Constants.KEY_LAST_NAME)));


        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });


        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful() &&task.getResult() != null){
                    sendFCMTokenToDatabase(task.getResult().getToken());
                }
            }
        });



        usersAdapter = new UsersAdapter(users, this);
        usersRecyclerView.setAdapter(usersAdapter);
        getUsers();


        swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        Handler handler = new Handler();

        refresh = new Runnable() {
            public void run() {
                //TODO: add online icon on users and refresh using this handler to show online and offline status
                handler.postDelayed(refresh, 5000);
            }
        };
        handler.post(refresh);

    }//onCreate ends

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_IS_ONLINE, Constants.KEY_ONLINE);
    }

    private void getUsers(){
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        swipeRefreshLayout.setRefreshing(false);

                        String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                        if (task.isSuccessful() && task.getResult() != null){
                            users.clear();
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()){
                                if (myUserId.equals(documentSnapshot.getId())){
                                    continue;
                                }
                                User user = new User();
                                user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                                user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                                user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                users.add(user);
                            }

                            if (users.size() > 0){
                                usersAdapter.notifyDataSetChanged();
                            }
                            else{
                                errorMessage.setText(String.format("%s", "No User Available"));
                                errorMessage.setVisibility(View.VISIBLE);
                            }
                        }
                        else{
                            errorMessage.setText(String.format("%s", "No User Available"));
                            errorMessage.setVisibility(View.VISIBLE);
                        }

                    }
                });
    }

    private void sendFCMTokenToDatabase(String token){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Token failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void signOut(){
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //sign out
                        //clearing preferences for making KEY_IS_SIGNED_IN = false
                        preferenceManager.clearPreferences();
                        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                        finish();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "unable to sign out", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void initiateVideoMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()){
            //user is offline
            Toast.makeText(this, String.format("%s %s %s",user.firstName ,user.lastName , "is not available for meeting"), Toast.LENGTH_LONG).show();
        }
        else{
            //user is online
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user",user);
            intent.putExtra("type","video");
            startActivity(intent);
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()){
            //user is offline
            Toast.makeText(this, String.format("%s %s %s",user.firstName ,user.lastName , "is not available for  meeting"), Toast.LENGTH_LONG).show();
        }
        else{
            //user is online
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user",user);
            intent.putExtra("type", "audio");
            startActivity(intent);

        }
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUserAction) {

        if (isMultipleUserAction){
            imageConference.setVisibility(View.VISIBLE);
            imageConference.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("selectedUsers",new Gson().toJson(usersAdapter.getSelectedUsers()));
                    intent.putExtra("type","video");
                    intent.putExtra("isMultiple",true);
                    startActivity(intent);
                }
            });
        }
        else{
            imageConference.setVisibility(View.GONE);
        }
    }


}






