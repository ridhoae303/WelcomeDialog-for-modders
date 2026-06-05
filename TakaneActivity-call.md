## Hardware Accelerated Implementation

    <!-- //OpenGL ES version -->
    <uses-feature
        android:glEsVersion="0x20000"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.vulkan.version"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen.multitouch"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen.multitouch.distinct"
        android:required="false" />
        
    // Call from any class, external.
     TakaneActivity.atsuko(this);
     
           <!-- // Add to AndroidManifest -->
           <activity
            android:name="com.takane.app.TakaneActivity"
            android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen"
            android:exported="false"
            android:hardwareAccelerated="true"
            android:configChanges="orientation|screenSize|keyboardHidden" />
