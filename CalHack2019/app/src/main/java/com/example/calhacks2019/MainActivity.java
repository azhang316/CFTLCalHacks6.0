package com.example.calhacks2019;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.mapbox.vision.VisionManager;
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener;
import com.mapbox.vision.mobile.core.models.AuthorizationStatus;
import com.mapbox.vision.mobile.core.models.Camera;
import com.mapbox.vision.mobile.core.models.Country;
import com.mapbox.vision.mobile.core.models.FrameSegmentation;
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications;
import com.mapbox.vision.mobile.core.models.detection.FrameDetections;
import com.mapbox.vision.mobile.core.models.position.VehicleState;
import com.mapbox.vision.mobile.core.models.road.RoadDescription;
import com.mapbox.vision.mobile.core.models.world.WorldDescription;
import com.mapbox.vision.safety.VisionSafetyManager;
import com.mapbox.vision.safety.core.VisionSafetyListener;
import com.mapbox.vision.safety.core.models.CollisionObject;
import com.mapbox.vision.safety.core.models.RoadRestrictions;
//import org.jetbrains.annotations.NotNull;
import android.support.annotation.NonNull;


public class MainActivity extends AppCompatActivity {

    private Float maxAllowedSpeed = -1f;

    // this listener handles events from Vision SDK
    private VisionEventsListener visionEventsListener = new VisionEventsListener() {

        @Override
        public void onAuthorizationStatusUpdated(@NonNull AuthorizationStatus authorizationStatus) {}

        @Override
        public void onFrameSegmentationUpdated(@NonNull FrameSegmentation frameSegmentation) {}

        @Override
        public void onFrameDetectionsUpdated(@NonNull FrameDetections frameDetections) {}

        @Override
        public void onFrameSignClassificationsUpdated(@NonNull FrameSignClassifications frameSignClassifications) {}

        @Override
        public void onRoadDescriptionUpdated(@NonNull RoadDescription roadDescription) {}

        @Override
        public void onWorldDescriptionUpdated(@NonNull WorldDescription worldDescription) {}

        @Override
        public void onVehicleStateUpdated(@NonNull VehicleState vehicleState) {
            // current speed of our car
            Float mySpeed = vehicleState.getSpeed();

            // display toast with overspeed warning if our speed is greater than maximum allowed speed
            if (mySpeed > maxAllowedSpeed && maxAllowedSpeed > 0) {
                Toast.makeText(
                        MainActivity.this,
                        "Overspeeding! Current speed : " + mySpeed +
                                ", allowed speed : " + maxAllowedSpeed,
                        Toast.LENGTH_LONG
                ).show();
            }
        }

        @Override
        public void onCameraUpdated(@NonNull Camera camera) {}

        @Override
        public void onCountryUpdated(@NonNull Country country) {}

        @Override
        public void onUpdateCompleted() {}
    };

    private VisionSafetyListener visionSafetyListener = new VisionSafetyListener() {
        @Override
        public void onCollisionsUpdated(@NonNull CollisionObject[] collisions) {}

        @Override
        public void onRoadRestrictionsUpdated(@NonNull RoadRestrictions roadRestrictions) {
            maxAllowedSpeed = roadRestrictions.getSpeedLimits().getCar().getMax();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        VisionManager.create();
        //VisionManager.start();
        //VisionManager.start(visionEventsListener);
        VisionManager.setVisionEventsListener(visionEventsListener);
        VisionManager.start();

        VisionSafetyManager.create(VisionManager.INSTANCE);
        VisionSafetyManager.setVisionSafetyListener(visionSafetyListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        VisionSafetyManager.destroy();

        VisionManager.stop();
        VisionManager.destroy();
    }
}

