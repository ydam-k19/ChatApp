package com.example.chatapp.activities;


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
import androidx.lifecycle.LifecycleOwner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Rational;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.example.chatapp.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class CameraActivity extends AppCompatActivity  implements View.OnClickListener {

    ImageButton imgCapture;
    ImageButton imgSwitchCamera;
    private CameraSelector lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    ProcessCameraProvider cameraProvider;
    PreviewView previewView;
    private ImageCapture imageCapture;

    private void flipCamera() {
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA)
            lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
        else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA)
            lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;
        startCameraX();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        imgCapture = findViewById(R.id.imgCapture);
        imgSwitchCamera = findViewById(R.id.imgSwitchCamera);
        previewView = findViewById(R.id.previewView);

        imgCapture.setOnClickListener(this);
        imgSwitchCamera.setOnClickListener(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                startCameraX();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, getExecutor());
    }

    private void startCameraX() {
        cameraProvider.unbindAll();

        Rational aspectRatio = new Rational(previewView.getWidth(), previewView.getHeight());
        Size screen = new Size(previewView.getWidth(), previewView.getHeight());

//        Preview use case
        Preview preview = new Preview.Builder().setTargetResolution(screen).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

//        image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        cameraProvider.bindToLifecycle((LifecycleOwner) this, lensFacing, preview, imageCapture);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgCapture:
                capturePhoto();
                break;
            case R.id.imgSwitchCamera:
                flipCamera();
                break;
        }
    }

    private void capturePhoto() {
        File photoFile = new File(getApplicationContext().getExternalCacheDir() + "/" + System.currentTimeMillis() + ".jpg");
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(CameraActivity.this, "Photo has been saved successfully", Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(CameraActivity.this, DsPhotoEditorActivity.class);
//                        intent.putExtra("path", photoFile.getAbsoluteFile() + "");
//                        startActivity(intent);

                        Uri inputImageUri = Uri.fromFile(new File(photoFile.getAbsoluteFile()+""));

                        Intent dsPhotoEditorIntent=new Intent(CameraActivity.this, DsPhotoEditorActivity.class);
                        dsPhotoEditorIntent.setData(inputImageUri);
                        dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY,"ChatApp Photo Directory");

                        int[] toolsToHide={DsPhotoEditorActivity.TOOL_ORIENTATION,DsPhotoEditorActivity.TOOL_CROP};
                        dsPhotoEditorIntent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE,toolsToHide);

                        startActivityForResult(dsPhotoEditorIntent, 200);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                }
        );
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            switch (requestCode){
                case 200:
                    Uri outputUri=data.getData();
                    Intent intent = new Intent(CameraActivity.this, ChatActivity.class);
                    intent.putExtra("imageUri", outputUri.toString());
                    setResult(RESULT_OK,intent);
                    finish();
                    break;
            }
        }
    }
}