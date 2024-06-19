package com.example.booleanfarmers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.processing.SurfaceProcessorNode;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Login_expert extends AppCompatActivity {

    private EditText lemail, lpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_expert);

        lemail = findViewById(R.id.l_email);
        lpass = findViewById(R.id.l_pass);

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
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Experts");

        // Query the database to find the user with the given email
        Query query = databaseReference.orderByChild("Email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User with the given email found, now check password
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dbPassword = snapshot.child("Password").getValue(String.class);
                        if (dbPassword != null && dbPassword.equals(password)) {
                            // Passwords match, login successful
                            Toast.makeText(Login_expert.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent=new Intent(Login_expert.this,View_page.class);
                            startActivity(intent);
                            // Proceed to next activity or perform necessary actions
                        } else {
                            // Password doesn't match
                            Toast.makeText(Login_expert.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // User with the given email not found
                    Toast.makeText(Login_expert.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Login_expert", "Database Error: " + databaseError.getMessage());
            }
        });
    }
}
