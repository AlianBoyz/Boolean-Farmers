package com.example.booleanfarmers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class farmer_signin extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private EditText signupName, signupDob, signupPhone, signupPassword, signupConfirmPassword, verificationCode;
    private Button signupButton, verifyButton;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_signin);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("farmers");

        signupName = findViewById(R.id.signup_name);
        signupDob = findViewById(R.id.signup_dob);
        signupPhone = findViewById(R.id.signup_phone);
        signupPassword = findViewById(R.id.signup_password);
        signupConfirmPassword = findViewById(R.id.signup_confirm_password);
        verificationCode = findViewById(R.id.verification_code);
        signupButton = findViewById(R.id.signup_button);
        verifyButton = findViewById(R.id.verify_button);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = signupName.getText().toString().trim();
                String dob = signupDob.getText().toString().trim();
                String phone = "+91" + signupPhone.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();
                String confirmPassword = signupConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    signupName.setError("Name cannot be empty");
                } else if (TextUtils.isEmpty(dob)) {
                    signupDob.setError("Date of Birth cannot be empty");
                } else if (TextUtils.isEmpty(phone)) {
                    signupPhone.setError("Phone number cannot be empty");
                } else if (TextUtils.isEmpty(password)) {
                    signupPassword.setError("Password cannot be empty");
                } else if (TextUtils.isEmpty(confirmPassword)) {
                    signupConfirmPassword.setError("Confirm Password cannot be empty");
                } else if (!password.equals(confirmPassword)) {
                    signupConfirmPassword.setError("Passwords do not match");
                } else {
                    sendVerificationCode(phone);
                }
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = verificationCode.getText().toString().trim();
                if (TextUtils.isEmpty(code)) {
                    verificationCode.setError("Verification code cannot be empty");
                } else {
                    verifyCode(code);
                }
            }
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            signInWithCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Toast.makeText(farmer_signin.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
            farmer_signin.this.verificationId = verificationId;
        }
    };

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String uid = auth.getCurrentUser().getUid();
                    String name = signupName.getText().toString().trim();
                    String dob = signupDob.getText().toString().trim();
                    String phone = signupPhone.getText().toString().trim();
                    String password = signupPassword.getText().toString().trim();

                    HashMap<String, String> farmerData = new HashMap<>();
                    farmerData.put("name", name);
                    farmerData.put("dob", dob);
                    farmerData.put("phone", phone);
                    farmerData.put("password", password);

                    databaseReference.child(uid).setValue(farmerData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(farmer_signin.this, "SignUp Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(farmer_signin.this, Login_farmer.class));
                                finish();
                            } else {
                                Log.e("DatabaseError", "Error: ", task.getException());
                                Toast.makeText(farmer_signin.this, "Database Update Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Log.e("AuthError", "Error: ", task.getException());
                    Toast.makeText(farmer_signin.this, "SignIn Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
