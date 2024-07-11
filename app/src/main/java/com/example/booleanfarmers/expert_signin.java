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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class expert_signin extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private EditText E_name, E_email, EU_name, E_pass, E_cpass;
    private Button e_sign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_signin);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("experts");

        E_name = findViewById(R.id.Name);
        EU_name = findViewById(R.id.username);
        E_email = findViewById(R.id.email);
        E_pass = findViewById(R.id.pass);
        E_cpass = findViewById(R.id.cpass);
        e_sign = findViewById(R.id.e_sign);

        e_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = E_name.getText().toString().trim();
                String username = EU_name.getText().toString().trim();
                String email = E_email.getText().toString().trim();
                String password = E_pass.getText().toString().trim();
                String confirmPassword = E_cpass.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    E_name.setError("Name cannot be empty");
                } else if (TextUtils.isEmpty(username)) {
                    EU_name.setError("Username cannot be empty");
                } else if (TextUtils.isEmpty(email)) {
                    E_email.setError("Email cannot be empty");
                } else if (TextUtils.isEmpty(password)) {
                    E_pass.setError("Password cannot be empty");
                } else if (TextUtils.isEmpty(confirmPassword)) {
                    E_cpass.setError("Confirm Password cannot be empty");
                } else if (!password.equals(confirmPassword)) {
                    E_cpass.setError("Passwords do not match");
                } else {
                    registerExpert(name, username, email, password);
                }
            }
        });
    }

    private void registerExpert(String name, String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(expert_signin.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(expert_signin.this, "User Registered", Toast.LENGTH_SHORT).show();
                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    assert firebaseUser != null;
                    firebaseUser.sendEmailVerification();

                    String uid = firebaseUser.getUid();
                    HashMap<String, String> expertData = new HashMap<>();
                    expertData.put("name", name);
                    expertData.put("username", username);
                    expertData.put("email", email);
                    expertData.put("password", password);

                    databaseReference.child(uid).setValue(expertData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(expert_signin.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(expert_signin.this, Login_expert.class));
                                finish();
                            } else {
                                Log.e("DatabaseError", "Error: ", task.getException());
                                Toast.makeText(expert_signin.this, "Database Update Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Log.e("AuthError", "Error: ", task.getException());
                    Toast.makeText(expert_signin.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
