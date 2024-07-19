package com.example.booleanfarmers;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class Post extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        imageView = findViewById(R.id.imageView);

        // Retrieve the image URI from the intent
        if (getIntent() != null && getIntent().hasExtra("imageUri")) {
            String imageUriString = getIntent().getStringExtra("imageUri");
            if (imageUriString != null) {
                Uri imageUri = Uri.parse(imageUriString);
                // Use Picasso to load the image from the URI
                Picasso.get().load(imageUri).into(imageView);
            }
        }
    }
}
