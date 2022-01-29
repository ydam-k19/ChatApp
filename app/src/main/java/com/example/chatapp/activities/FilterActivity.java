package com.example.chatapp.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.chatapp.R;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFrontFacingFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FilterActivity extends AppCompatActivity {

    public String pathModel = "";
    public String pathTextures = "";
    private Set<CompletableFuture<?>> loaders = new HashSet<>();

    //    private ActivityFilterBinding binding;
    private ArFrontFacingFragment arFragment;
    private ArSceneView arSceneView;

    FrameLayout frameLayout;
    private Texture faceTexture;
    private ModelRenderable faceModel;

    private HashMap<AugmentedFace, AugmentedFaceNode> facesNodes = new HashMap<>();


    // them code
    private ImageButton capture_img, anonymous_filter, fox_filter, cat_filter, canonical_filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filter);
        capture_img = findViewById(R.id.capture_imgbtn);
        anonymous_filter = findViewById(R.id.anonymous_filter);
        fox_filter = findViewById(R.id.fox_filter);
        cat_filter = findViewById(R.id.cat_filter);
        canonical_filter = findViewById(R.id.canonical_filter);

        verifyStoragePermission(this);

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
                Log.d("@@@", "onclick");

                loaders.clear();

                for (AugmentedFace augmentedFaceNode : facesNodes.keySet()) {
                    arSceneView.getScene().removeChild(facesNodes.get(augmentedFaceNode));
                    facesNodes.remove(augmentedFaceNode);
                }
                faceTexture = null;
                faceModel = null;
                pathModel = "models/mask.glb";
                pathTextures = "textures/mask_2.png";
//                Log.d("anonymous",pathModel);

                loadModels();
                loadTextures();
            }
        });

        fox_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loaders.clear();

                for (AugmentedFace augmentedFaceNode : facesNodes.keySet()) {
                    arSceneView.getScene().removeChild(facesNodes.get(augmentedFaceNode));
                    facesNodes.remove(augmentedFaceNode);
                }
                faceTexture = null;
                faceModel = null;
                pathModel = "models/fox.glb";
                pathTextures = "textures/freckles.png";
//                Log.d("anonymous",pathModel);

                loadModels();
                loadTextures();
            }
        });

        canonical_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loaders.clear();

                for (AugmentedFace augmentedFaceNode : facesNodes.keySet()) {
                    arSceneView.getScene().removeChild(facesNodes.get(augmentedFaceNode));
                    facesNodes.remove(augmentedFaceNode);
                }
                faceTexture = null;
                faceModel = null;
                pathModel = "models/canonical_face.glb";
                pathTextures = "textures/canonical_face.png";
//                Log.d("anonymous",pathModel);

                loadModels();
                loadTextures();
            }
        });
        cat_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loaders.clear();

                for (AugmentedFace augmentedFaceNode : facesNodes.keySet()) {
                    arSceneView.getScene().removeChild(facesNodes.get(augmentedFaceNode));
                    facesNodes.remove(augmentedFaceNode);
                }
                faceTexture = null;
                faceModel = null;
                pathModel = "models/face.glb";
                pathTextures = "textures/face.png";
//                Log.d("anonymous",pathModel);

                loadModels();
                loadTextures();
            }
        });

        capture_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeScreenShot();
            }
        });
    }

    public void func() {
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


    private void takeScreenShot() {

        Frame currentFrame = arSceneView.getArFrame();
        View v = arSceneView.getRootView();
        Image currentImage = null;
        try {
            currentImage = currentFrame.acquireCameraImage();
        } catch (NotYetAvailableException e) {
            e.printStackTrace();
        }
        int imageFormat = currentImage.getFormat();
        if (imageFormat == ImageFormat.YUV_420_888) {
            Log.d("ImageFormat", "Image format is YUV_420_888");
        }
        byte[] b = imageToByte(currentImage);

        Bitmap bitmap= BitmapFactory.decodeByteArray(b , 0, b.length);
        currentImage.close();

//        rotate bitmap
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        Intent intent = new Intent(FilterActivity.this, ChatActivity.class);
            intent.putExtra("img_filter", encodeImage(rotatedBitmap));
            setResult(RESULT_OK,intent);
            finish();
    }

    private static final int REQUEST_EXTERNAL_STORAGE=1;
    private static String[] PERMISSION_STORAGE={Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};

    public void verifyStoragePermission(Activity activity){
        int permission = ActivityCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permission!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity, PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    private static byte[] imageToByte(Image image){
        byte[] byteArray = null;
        byteArray = NV21toJPEG(YUV420toNV21(image),image.getWidth(),image.getHeight(),100);
        return byteArray;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();


        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }
}