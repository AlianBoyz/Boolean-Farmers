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

public class Login_expert extends AppCompatActivity {

    private EditText lemail, lpass;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_expert);

        lemail = findViewById(R.id.l_email);
        lpass = findViewById(R.id.l_pass);
        auth = FirebaseAuth.getInstance();

        Button e_sign = findViewById(R.id.e_sign);
        Button e_login = findViewById(R.id.e_login);

        e_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_expert.this, expert_signin.class);
                startActivity(intent);
            }
        });

        e_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = lemail.getText().toString().trim();
                String password = lpass.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(Login_expert.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                authenticateUser(email, password);
            }
        });
    }

    private void authenticateUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Login successful
                    Toast.makeText(Login_expert.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Login_expert.this, problem_screen.class);
                    startActivity(intent);
                    finish(); // Close the current activity
                } else {
                    // Login failed
                    Log.e("Login_expert", "Login Failed: " + task.getException().getMessage());
                    Toast.makeText(Login_expert.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}


