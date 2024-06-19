package com.example.booleanfarmers;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class expert_signin extends AppCompatActivity {

    EditText E_name,E_email,EU_name,E_pass,E_cpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_signin);

        E_name=findViewById(R.id.Name);
        EU_name=findViewById(R.id.username);
        E_email=findViewById(R.id.email);
        E_pass=findViewById(R.id.pass);
        E_cpass=findViewById(R.id.cpass);

        Button e_sign=findViewById(R.id.e_sign);

        e_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String n=E_name.getText().toString();
                String u=EU_name.getText().toString();
                String e=E_email.getText().toString();
                String p=E_pass.getText().toString();
                String cp=E_cpass.getText().toString();

                if (TextUtils.isEmpty(n) || TextUtils.isEmpty(u)||TextUtils.isEmpty(e) || TextUtils.isEmpty(p)||TextUtils.isEmpty(cp)) {
                    Toast.makeText(expert_signin.this, "Fill all the fields.",Toast.LENGTH_SHORT).show();
                } else if (!p.equals(cp)) {
                    Toast.makeText(expert_signin.this, "Passwords don't match ",Toast.LENGTH_SHORT).show();
                } else {
                    registerExpert(n, u, e, p);
                }
            }
        });
    }

    private void registerExpert(String name, String username, String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(expert_signin.this, new OnCompleteListener<AuthResult>() {
            @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Toast.makeText(expert_signin.this, "User Registered", Toast.LENGTH_SHORT).show();
                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    assert firebaseUser != null;
                    firebaseUser.sendEmailVerification();

                    Intent intent=new Intent(expert_signin.this, Login_expert.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    Toast.makeText(expert_signin.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
       Toast.makeText(expert_signin.this, "Registered.", Toast.LENGTH_SHORT).show();
       Intent intent = new Intent(expert_signin.this, Login_expert.class);
       startActivity(intent);
    }
}
