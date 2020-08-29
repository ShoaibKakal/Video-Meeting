package com.shoaib.videomeeting.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shoaib.videomeeting.R;
import com.shoaib.videomeeting.utilities.Constants;
import com.shoaib.videomeeting.utilities.PreferenceManager;

import java.util.HashMap;


public class SignUpActivity extends AppCompatActivity {

    private ImageView backImageIcon;
    private EditText inputFirstName, inputLastName, inputEmail, inputPassword, inputConfirmPassword;
    private MaterialButton buttonSignUp;
    private ProgressBar signUpProgressBar;

    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        preferenceManager = new PreferenceManager(getApplicationContext());


        backImageIcon = findViewById(R.id.imageBack);
        inputFirstName = findViewById(R.id.inputFirstName);
        inputLastName = findViewById(R.id.inputLastName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);

        backImageIcon.setOnClickListener(v -> onBackPressed());

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputFirstName.getText().toString().trim().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Enter First Name", Toast.LENGTH_SHORT).show();
                }
                else if (inputLastName.getText().toString().trim().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Enter Last Name", Toast.LENGTH_SHORT).show();
                }
                else if (inputEmail.getText().toString().trim().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                }
                else if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString()).matches()){
                    Toast.makeText(SignUpActivity.this, "Enter valid email", Toast.LENGTH_SHORT).show();
                }
                else if (inputPassword.getText().toString().trim().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                }
                else if (inputConfirmPassword.getText().toString().trim().isEmpty()){
                    Toast.makeText(SignUpActivity.this, "Confirm Your Password", Toast.LENGTH_SHORT).show();
                }
                else if (!inputPassword.getText().toString().trim().equals(inputConfirmPassword.getText().toString().trim())){
                    Toast.makeText(SignUpActivity.this, "Password & Confirm Password must be same", Toast.LENGTH_SHORT).show();
                }
                else{
                    // good to go
                    signUp();
                }

            }
        });
    } // onCreate ends

    private void signUp(){

        buttonSignUp.setVisibility(View.INVISIBLE);
        signUpProgressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString().trim());
        user.put(Constants.KEY_LAST_NAME, inputLastName.getText().toString().trim());
        user.put(Constants.KEY_EMAIL, inputEmail.getText().toString().trim());
        user.put(Constants.KEY_PASSWORD, inputPassword.getText().toString().trim());
        user.put(Constants.KEY_IS_ONLINE, true);

        db.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString().trim());
                        preferenceManager.putString(Constants.KEY_LAST_NAME, inputLastName.getText().toString().trim());
                        preferenceManager.putString(Constants.KEY_EMAIL, inputEmail.getText().toString().trim());
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        signUpProgressBar.setVisibility(View.INVISIBLE);
                        buttonSignUp.setVisibility(View.VISIBLE);
                        Toast.makeText(SignUpActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }
    public void openSignIn(View view) {

        startActivity(new Intent(getApplicationContext(), SignInActivity.class));

    }
}