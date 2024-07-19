package com.example.booleanfarmers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    private ImageCapture imageCapture;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth auth;
    private TextView usernameTextView;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraPermission = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    cameraPermission = result.getOrDefault(Manifest.permission.CAMERA, false);
                }

                if (cameraPermission) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission not granted by the user.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Camera permission not granted: " + cameraPermission);
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
        usernameTextView = findViewById(R.id.UserName);
        firebaseStorage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA
            });
        }

        captureButton.setOnClickListener(view -> takePhoto());
        browseButton.setOnClickListener(view -> openGallery());
        logout.setOnClickListener(view -> funcLogout());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void funcLogout() {
        auth.signOut();
        Toast.makeText(Capture.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Capture.this, Login_farmer.class);
        startActivity(intent);
        finish();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        }

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

        CameraSelector cameraSelector = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        }
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    // Convert ImageProxy to bitmap
                    Bitmap capturedBitmap = imageProxyToBitmap(image);

                    // Resize bitmap to 256x256
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(capturedBitmap, 256, 256, true);

                    // Convert resized bitmap to byte array
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] resizedBytes = stream.toByteArray();

                    // Upload resized image to Firebase
                    uploadImageToFirebase(resizedBytes);

                    image.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Error capturing image", exception);
                }
            });
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            planeProxy = image.getPlanes()[0];
        }
        ByteBuffer buffer = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buffer = planeProxy.getBuffer();
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }



    private void uploadImageToFirebase(byte[] imageData) {
        StorageReference storageReference = firebaseStorage.getReference().child("images/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = storageReference.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Image uploaded to Firebase: " + downloadUrl);
                Toast.makeText(Capture.this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show();
                openPostActivity(downloadUrl);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to upload image to Firebase", e);
            Toast.makeText(Capture.this, "Failed to upload image to Firebase", Toast.LENGTH_SHORT).show();
        });
    }



    private void openPostActivity(String imageUrl) {
        Intent intent = new Intent(Capture.this, Post.class);
        intent.putExtra("imageUri", imageUrl);
        startActivity(intent);
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
            }
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, false);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            StorageReference storageReference = firebaseStorage.getReference().child("images/" + System.currentTimeMillis() + ".jpg");
            UploadTask uploadTask = storageReference.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Image uploaded to Firebase: " + downloadUrl);
                    Toast.makeText(Capture.this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show();
                    openPostActivity(downloadUrl);
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload image to Firebase", e);
                Toast.makeText(Capture.this, "Failed to upload image to Firebase", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to resize and upload image", e);
            Toast.makeText(Capture.this, "Failed to resize and upload image", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
