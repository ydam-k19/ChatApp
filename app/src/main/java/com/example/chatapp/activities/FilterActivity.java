package com.example.chatapp.activities;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chatapp.R;

import com.google.ar.core.AugmentedFace;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFrontFacingFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FilterActivity extends AppCompatActivity {

    public  String pathModel="models/fox.glb";
    public  String pathTextures="textures/freckles.png";
    private  Set<CompletableFuture<?>> loaders = new HashSet<>();

//    private ActivityFilterBinding binding;
    private ArFrontFacingFragment arFragment;
    private ArSceneView arSceneView;

    FrameLayout frameLayout;
    private Texture faceTexture;
    private ModelRenderable faceModel;

    private  HashMap<AugmentedFace, AugmentedFaceNode> facesNodes = new HashMap<>();


    // them code
    private ImageButton capture_img,anonymous_filter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filter);
        capture_img= findViewById(R.id.capture);
        anonymous_filter=findViewById(R.id.anonymous_filter);
//        capture_img=binding.capture;
//        frameLayout=binding.arFragment;

        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFrontFacingFragment.class, null)
                        .commit();
            }
        }

        loadModels();
        loadTextures();

        anonymous_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loaders.clear();

                for(AugmentedFace augmentedFaceNode : facesNodes.keySet()){
                    arSceneView.getScene().removeChild(facesNodes.get(augmentedFaceNode));
                    facesNodes.remove(augmentedFaceNode);
                }
                faceTexture=null;
                faceModel=null;
                pathModel="models/mask.glb";
                pathTextures="textures/mask_2.png";
//                Log.d("anonymous",pathModel);

                loadModels();
                loadTextures();
            }
        });
        capture_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(FilterActivity.this,Custom_CameraActivity.class);
                startActivity(intent);
            }
        });
    }

    public void func(){
        getSupportFragmentManager().addFragmentOnAttachListener(this::onAttachFragment);
    }

    public Bitmap viewToBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 500;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFrontFacingFragment) fragment;
            arFragment.setOnViewCreatedListener(this::onViewCreated);
        }
    }

    public void onViewCreated(ArSceneView arSceneView) {
        this.arSceneView = arSceneView;

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

        // Check for face detections
        arFragment.setOnAugmentedFaceUpdateListener(this::onAugmentedFaceTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (CompletableFuture<?> loader : loaders) {
            if (!loader.isDone()) {
                loader.cancel(true);
            }
        }
    }

    private void loadModels() {

        loaders.add(ModelRenderable.builder()
                .setSource(this, Uri.parse(pathModel))

                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> faceModel = model)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                    return null;
                }));

    }

    private void loadTextures() {

        loaders.add(Texture.builder()
                .setSource(this, Uri.parse(pathTextures))
                .setUsage(Texture.Usage.COLOR_MAP)
                .build()
                .thenAccept(texture -> faceTexture = texture)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Unable to load texture", Toast.LENGTH_LONG).show();
                    return null;
                }));


    }

    public void onAugmentedFaceTrackingUpdate(AugmentedFace augmentedFace) {
        if (faceModel == null || faceTexture == null) {
            return;
        }

        AugmentedFaceNode existingFaceNode = facesNodes.get(augmentedFace);

        switch (augmentedFace.getTrackingState()) {
            case TRACKING:
                if (existingFaceNode == null) {
                    AugmentedFaceNode faceNode = new AugmentedFaceNode(augmentedFace);

                    RenderableInstance modelInstance = faceNode.setFaceRegionsRenderable(faceModel);
                    modelInstance.setShadowCaster(false);
                    modelInstance.setShadowReceiver(true);

                    faceNode.setFaceMeshTexture(faceTexture);

                    arSceneView.getScene().addChild(faceNode);

                    facesNodes.put(augmentedFace, faceNode);
                }


                break;
            case STOPPED:
                if (existingFaceNode != null) {
                    arSceneView.getScene().removeChild(existingFaceNode);
                }
                facesNodes.remove(augmentedFace);
                break;
        }
    }

}