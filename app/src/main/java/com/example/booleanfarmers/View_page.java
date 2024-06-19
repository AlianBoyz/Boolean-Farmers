package com.example.booleanfarmers;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class View_page extends AppCompatActivity {

    private TextView user_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_page);

        user_name = findViewById(R.id.uname);

        // Get a reference to the Firebase database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Experts");

        // Read the expert's name from the database
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Iterate through all children (each expert)
                    for (DataSnapshot expertSnapshot : dataSnapshot.getChildren()) {
                        String name = expertSnapshot.child("Name").getValue(String.class);
                        // Check if name is not null to avoid NullPointerException
                        if (name != null) {
                            // Append the retrieved name to the TextView
                            user_name.append(name + "\n");
                        }
                    }
                } else {
                    // Handle the case where no expert exists in the database
                    user_name.setText("No experts found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors that may occur
                user_name.setText("Error: " + databaseError.getMessage());
            }
        });
    }
}
