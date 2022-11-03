package com.google.ar.sceneform.samples.augmentedimages;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.android.filament.Engine;
import com.google.android.filament.filamat.MaterialBuilder;
import com.google.android.filament.filamat.MaterialPackage;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivityQR extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private boolean matrixDetected = false;
    private boolean rabbitDetected = false;
    private boolean laptop1Detected = false;
    ViewRenderable arFurDetails,arFurDetails2,arFurDetails3 ;
    private AugmentedImageDatabase database;
    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_qr);
        Toolbar toolbar = findViewById(R.id.toolbar3);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                    .top;

            return WindowInsetsCompat.CONSUMED;
        });
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment3, ArFragment.class, null)
                        .commit();
            }
        }

        if(Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts

            loadMatrixModel();
            loadMatrixMaterial();
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment3) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        database = new AugmentedImageDatabase(session);

        Bitmap matrixImage = BitmapFactory.decodeResource(getResources(), R.drawable.matrix);
        Bitmap rabbitImage = BitmapFactory.decodeResource(getResources(), R.drawable.rabbit);
        Bitmap laptop1 = BitmapFactory.decodeResource(getResources(), R.drawable.qr_laptop1);
        Bitmap laptop2 = BitmapFactory.decodeResource(getResources(), R.drawable.qr_laptop2);
        Bitmap laptop3 = BitmapFactory.decodeResource(getResources(), R.drawable.qr_laptop3);

        // Every image has to have its own unique String identifier
        database.addImage("matrix", matrixImage);
        database.addImage("rabbit", rabbitImage);
        database.addImage("Laptop 1", laptop1);
        database.addImage("Laptop 2", laptop2);
        database.addImage("Laptop 3", laptop3);


        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    private void loadMatrixModel() {
        Toast.makeText(getApplicationContext(), "Loading video", Toast.LENGTH_SHORT).show();
        WeakReference<MainActivityQR> weakActivity = new WeakReference<>(this);

        ViewRenderable.builder()
                .setView(this, R.layout.text1)
                .build().thenAccept(renderable -> arFurDetails = renderable);


        futures.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/Video.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    //removing shadows for this Renderable
                    model.setShadowCaster(false);
                    model.setShadowReceiver(true);
                    plainVideoModel = model;
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }
    private void loadMatrixMaterial() {
        Toast.makeText(getApplicationContext(), "load matrix", Toast.LENGTH_SHORT).show();

        Engine filamentEngine = EngineInstance.getEngine().getFilamentEngine();

        MaterialBuilder.init();
        MaterialBuilder materialBuilder = new MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("External Video Material")
                .require(MaterialBuilder.VertexAttribute.UV0)
                .shading(MaterialBuilder.Shading.UNLIT)
                .doubleSided(true)
                .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_EXTERNAL, MaterialBuilder.SamplerFormat.FLOAT, MaterialBuilder.ParameterPrecision.DEFAULT, "videoTexture")
                .optimization(MaterialBuilder.Optimization.NONE);

        MaterialPackage plainVideoMaterialPackage = materialBuilder
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n")
                .build(filamentEngine);
        if (plainVideoMaterialPackage.isValid()) {
            ByteBuffer buffer = plainVideoMaterialPackage.getBuffer();
            futures.add(Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept(material -> {
                        plainVideoMaterial = material;
                    })
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG).show();
                                return null;
                            }));
        }
        MaterialBuilder.shutdown();
    }

    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (matrixDetected && rabbitDetected) {
            return;
        }



        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            // Setting anchor to the center of Augmented Image
           // Anchor anchor = augmentedImage.createAnchor(Pose.IDENTITY);
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            // If matrix video haven't been placed yet and detected image has String identifier of "matrix"
            if (!matrixDetected && augmentedImage.getName().equals("matrix")) {
                matrixDetected = true;
                Toast.makeText(this, "Matrix tag detected", Toast.LENGTH_LONG).show();

                // AnchorNode placed to the detected tag and set it to the real size of the tag
                // This will cause deformation if your AR tag has different aspect ratio than your video
                anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
                // For some reason it is shown upside down so this will rotate it correctly
                videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 180f));
                anchorNode.addChild(videoNode);

                // Setting texture
                ExternalTexture externalTexture = new ExternalTexture();
                RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
                renderableInstance.setMaterial(plainVideoMaterial);

                // Setting MediaPLayer
                renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);
                mediaPlayer = MediaPlayer.create(this, R.raw.matrix);
                mediaPlayer.setLooping(true);
                mediaPlayer.setSurface(externalTexture.getSurface());
                mediaPlayer.start();
            }
            // If rabbit model haven't been placed yet and detected image has String identifier of "rabbit"
            // This is also example of model loading and placing at runtime
            if (!laptop1Detected && augmentedImage.getName().equals("Laptop 1")) {
                laptop1Detected = true;
                Toast.makeText(this, "LAPTOP 1 DETECTED", Toast.LENGTH_LONG).show();


                //textdisplay in AR
                Anchor anchorText = augmentedImage.createAnchor(Pose.IDENTITY);
                AnchorNode anchorNodeText = new AnchorNode(anchorText);
                anchorNodeText.setParent(arFragment.getArSceneView().getScene());


                TransformableNode displayText = new TransformableNode(arFragment.getTransformationSystem());
                displayText.setParent(anchorNodeText);
                RenderableInstance modelInstance = displayText.setRenderable(this.arFurDetails);
                modelInstance.getMaterial().setInt("baseColorIndex", 0);
                displayText.select();
                title(anchorNode, displayText, "LAPTOP 1\n\n" +
                        "Only for Junior Employees\n\n" +
                        "TAKE IT?\n\nTAPE TO CONFIRM");

//                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
//                arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//
//
//
//                TransformableNode detailsText2 = new TransformableNode(arFragment.getTransformationSystem());
//                detailsText2.setParent(anchorNode);
//                detailsText2.setRenderable(arFurDetails);
//                detailsText2.setLocalPosition(new Vector3(0.5f,  0.5f, 0));
//                anchorNode.addChild(detailsText2);
//
//                TextView arFurDetailsTV = (TextView) arFurDetails.getView();
//                arFurDetailsTV.setText("Please Pick a Mouse");
//                arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                       // Toast.makeText(getApplicationContext(), "Details Selected", Toast.LENGTH_SHORT).show();
//                        //anchorNodeText.setParent(null);
//                        //title3();
//                    }
//                });

//                futures.add(ModelRenderable.builder()
//                        .setSource(this, Uri.parse("models/laptop.glb"))
//                        .setIsFilamentGltf(true)
//                        .build()
//                        .thenAccept(LaptopModel -> {
//                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                            modelNode.setRenderable(LaptopModel);
//                            anchorNode.addChild(modelNode);
//                        })
//                        .exceptionally(
//                                throwable -> {
//                                    Toast.makeText(this, "Unable to load laptop model", Toast.LENGTH_LONG).show();
//                                    return null;
//                                }));



                //textdisplay in AR

//                TransformableNode displayText = new TransformableNode(arFragment.getTransformationSystem());
//                displayText.select();
                //title2();
//                title(anchorNode, displayText, "LAPTOP 1\n\n" +
//                        "Only for Junior Employees\n\n" +
//                        "TAKE IT?\n\nTAPE TO CONFIRM");
//                tukatitle(anchorNode, displayText, "");
//                tukatitle2(anchorNode, displayText, "");

            }
        }
        if (matrixDetected && laptop1Detected) {
            arFragment.getInstructionsController().setEnabled(
                    InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }
    }

    private void title(AnchorNode anchorNodeText, TransformableNode displayText, String furniture) {

        TransformableNode detailsViewAR = new TransformableNode(arFragment.getTransformationSystem());
        detailsViewAR.setRenderable(arFurDetails);
        detailsViewAR.setLocalPosition(new Vector3(0f, displayText.getLocalPosition().y + 1f, 0));
        anchorNodeText.addChild(detailsViewAR);

        TextView arFurDetailsTV = (TextView) arFurDetails.getView();
        arFurDetailsTV.setText(furniture);
        arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "NEXT STEP", Toast.LENGTH_SHORT).show();
                //anchorNodeText.setParent(null);
                title2();
            }
        });
    }
    private void title3() {

        TransformableNode displayText2 = new TransformableNode(arFragment.getTransformationSystem());
        displayText2.select();

        TransformableNode detailsText2 = new TransformableNode(arFragment.getTransformationSystem());
        detailsText2.setRenderable(arFurDetails);
        detailsText2.setLocalPosition(new Vector3(1f, displayText2.getLocalPosition().y + 1f, 0));
        detailsText2.select();

        TextView arFurDetailsTV = (TextView) arFurDetails.getView();
        arFurDetailsTV.setText("LAPTOP OBTAINED SUCCESSFULLY");
        arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "COMPLETE", Toast.LENGTH_SHORT).show();
                //anchorNodeText.setParent(null);

            }
        });
    }
    private void title2() {

        TransformableNode displayText2 = new TransformableNode(arFragment.getTransformationSystem());
        displayText2.select();

        TransformableNode detailsText2 = new TransformableNode(arFragment.getTransformationSystem());
        detailsText2.setRenderable(arFurDetails);
        detailsText2.setLocalPosition(new Vector3(0.5f, displayText2.getLocalPosition().y + 1f, 0));
        detailsText2.select();

        TextView arFurDetailsTV = (TextView) arFurDetails.getView();
        arFurDetailsTV.setText("Please Pick a Mouse");
        arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Toast.makeText(getApplicationContext(), "Details Selected", Toast.LENGTH_SHORT).show();
                //anchorNodeText.setParent(null);
                title3();
            }
        });
    }
    private void tukatitle2(AnchorNode anchorNode, TransformableNode displayText, String s) {
        TransformableNode detailsViewAR = new TransformableNode(arFragment.getTransformationSystem());
        detailsViewAR.setRenderable(arFurDetails3);
        detailsViewAR.setLocalPosition(new Vector3(0.5f, displayText.getLocalPosition().y + 1.5f, 0));
        detailsViewAR.setParent(anchorNode);
        detailsViewAR.select();

        TextView arFurDetailsTV = (TextView) arFurDetails3.getView();
        arFurDetailsTV.setText(s);
        arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "TUKAAA Selected", Toast.LENGTH_SHORT).show();


            }
        });
    }
    private void tukatitle(AnchorNode anchorNode, TransformableNode displayText, String hoi) {
        TransformableNode detailsViewAR = new TransformableNode(arFragment.getTransformationSystem());
        detailsViewAR.setRenderable(arFurDetails2);
        detailsViewAR.setLocalPosition(new Vector3(0.3f, displayText.getLocalPosition().y + 1f, 0));
        detailsViewAR.setParent(anchorNode);
        detailsViewAR.select();

        TextView arFurDetailsTV = (TextView) arFurDetails2.getView();
        arFurDetailsTV.setText(hoi);
        arFurDetailsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "TUKAAA Selected", Toast.LENGTH_SHORT).show();


            }
        });
    }

}