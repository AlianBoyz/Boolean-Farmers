package com.example.booleanfarmers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class farmer_signin extends AppCompatActivity {

    EditText F_name,F_dob,F_pass,F_cpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_signin);

        F_name=findViewById(R.id.Name);
        F_dob=findViewById(R.id.Dob);
        F_pass=findViewById(R.id.Pass);
        F_cpass=findViewById(R.id.Conf_pass);

        Button f_sign_in=findViewById(R.id.f_sign_in);

        f_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String n=F_name.getText().toString();
                String d=F_dob.getText().toString();
                String p=F_pass.getText().toString();
                String cp=F_cpass.getText().toString();

                if (TextUtils.isEmpty(n)|| TextUtils.isEmpty(p)||TextUtils.isEmpty(cp)) {
                    Toast.makeText(farmer_signin.this, "Fill all the fields.",Toast.LENGTH_SHORT).show();
                } else if (!p.equals(cp)) {
                    Toast.makeText(farmer_signin.this, "Passwords don't match ",Toast.LENGTH_SHORT).show();
                } else {
                    registerFarmer(n,d, p);
                }
            }
        });
    }
    private void registerFarmer(String name, String dob, String password) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Farmers");

        // Assuming email is unique and can be used as a key
        databaseReference.child(name.replace(".", "_")).child("Name").setValue(name);
        databaseReference.child(name.replace(".", "_")).child("Date of Birth").setValue(dob);
        databaseReference.child(name.replace(".", "_")).child("Password").setValue(password);

        Toast.makeText(farmer_signin.this, "Registered.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(farmer_signin.this, Login_farmer.class);
        startActivity(intent);
    }
}
