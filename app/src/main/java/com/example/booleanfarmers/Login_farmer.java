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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Login_farmer extends AppCompatActivity {

    private EditText lname, lpass;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_farmer);

        lname = findViewById(R.id.l_namef);
        lpass = findViewById(R.id.l_passf);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("farmers");

        Button f_sign = findViewById(R.id.f_sign);
        Button f_login = findViewById(R.id.f_login);

        f_sign.setOnClickListener(v -> startActivity(new Intent(Login_farmer.this, farmer_signin.class)));

        f_login.setOnClickListener(v -> {
            String phone = lname.getText().toString().trim();
            String password = lpass.getText().toString().trim();
            if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
                Toast.makeText(Login_farmer.this, "Please enter phone number and password", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticateUser(phone, password);
        });
    }

    private void authenticateUser(String phone, String password) {
        Query query = databaseReference.orderByChild("phone").equalTo(phone);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String dbPassword = snapshot.child("password").getValue(String.class);
                        if (dbPassword != null && dbPassword.equals(password)) {
                            Toast.makeText(Login_farmer.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Login_farmer.this, Capture.class);
                            intent.putExtra("uname", phone.toString());
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                    Toast.makeText(Login_farmer.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Login_farmer.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Login_farmer", "Database Error: " + databaseError.getMessage());
            }
        });
    }
}
