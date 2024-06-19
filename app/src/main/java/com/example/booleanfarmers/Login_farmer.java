package com.example.booleanfarmers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
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

public class Login_farmer extends AppCompatActivity {

    private EditText lname, lpass;

    DatabaseReference reference;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_farmer);

        lname = findViewById(R.id.l_namef);
        lpass = findViewById(R.id.l_passf);

        Button f_sign = findViewById(R.id.f_sign);
        Button f_login = findViewById(R.id.f_login);

        f_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_farmer.this, farmer_signin.class);
                startActivity(intent);
            }
        });

        f_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = lname.getText().toString().trim();
                String password = lpass.getText().toString().trim();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
                    Toast.makeText(Login_farmer.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                authenticateUser(name, password);
            }
        });
    }
    private void authenticateUser(String name, String password) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Farmers");

        // Query the database to find the user with the given email
        Query query = databaseReference.orderByChild("Name").equalTo(name);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User with the given email found, now check password
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dbPassword = snapshot.child("Password").getValue(String.class);
                        if (dbPassword != null && dbPassword.equals(password)) {
                            // Passwords match, login successful
                            Toast.makeText(Login_farmer.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent=new Intent(Login_farmer.this,Capture.class);
                            intent.putExtra("uname",name);
                            startActivity(intent);
                            finish();
                            // Proceed to next activity or perform necessary actions
                        } else {
                            // Password doesn't match
                            Toast.makeText(Login_farmer.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // User with the given email not found
                    Toast.makeText(Login_farmer.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Login_expert", "Database Error: " + databaseError.getMessage());
            }
        });
    }
}
