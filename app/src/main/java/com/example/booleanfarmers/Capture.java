package com.example.booleanfarmers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Capture extends AppCompatActivity {

    private static final String TAG = "Capture";
    private static final int REQUEST_CODE_IMAGE_FROM_GALLERY = 20;
    private static final int REQUEST_CODE_IMAGE_CAPTURE = 21;

    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private Button logout;
    private Button captureButton;
    private Button browseButton;
    private TextView userName;
    private ImageCapture imageCapture;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraPermission = result.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean readStoragePermission = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);

                if (cameraPermission && readStoragePermission) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Permissions not granted: " +
                            "Camera: " + cameraPermission + ", " +
                            "Read Storage: " + readStoragePermission);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        browseButton = findViewById(R.id.browseButton);
        logout = findViewById(R.id.logout_button);
        userName = findViewById(R.id.UserName); // Assuming you have this TextView in your layout

        // Initialize Firebase components
        firebaseStorage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(Capture.this, "User details not available", Toast.LENGTH_LONG).show();
        } else {
            fetchUserProfile(user);
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }

        captureButton.setOnClickListener(view -> takePhoto());
        browseButton.setOnClickListener(view -> openGallery());
        logout.setOnClickListener(view -> funcLogout());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void fetchUserProfile(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("farmers").child(uid);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) {
                        userName.setText(name);
                    } else {
                        Toast.makeText(Capture.this, "User name not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Capture.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Capture.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to fetch user data", error.toException());
            }
        });
    }


    private void funcLogout() {
        auth.signOut();
        Toast.makeText(Capture.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Capture.this, Login_farmer.class);
        startActivity(intent);
        finish();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void takePhoto() {
        File photoDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BooleanFarmers");
        if (!photoDir.exists() && !photoDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory for photos");
            return;
        }

        File photoFile = new File(photoDir, "image_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo saved: " + savedUri;
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                uploadImageToFirebase(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Error capturing image", exception);
            }
        });
    }

    private void uploadImageToFirebase(File photoFile) {
        StorageReference storageReference = firebaseStorage.getReference().child("images/" + photoFile.getName());
        try {
            FileInputStream fileInputStream = new FileInputStream(photoFile);
            UploadTask uploadTask = storageReference.putStream(fileInputStream);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Image uploaded to Firebase: " + downloadUrl);
                    Toast.makeText(Capture.this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload image to Firebase", e);
                Toast.makeText(Capture.this, "Failed to upload image to Firebase", Toast.LENGTH_SHORT).show();
            });
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for upload", e);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_IMAGE_FROM_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMAGE_FROM_GALLERY && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    uploadImageToFirebase(selectedImageUri);
                }
            } else if (requestCode == REQUEST_CODE_IMAGE_CAPTURE) {
                // Image captured from camera, already handled in takePhoto()
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        StorageReference storageReference = firebaseStorage.getReference().child("images/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = storageReference.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Image uploaded to Firebase: " + downloadUrl);
                Toast.makeText(Capture.this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image to Firebase", e);
            Toast.makeText(Capture.this, "Failed to upload image to Firebase", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
