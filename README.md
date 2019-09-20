# Reference Android App demonstrating the use of [Automated License Plate Recognition library](http://www.openalpr.com/)

# Reference Android App https://github.com/sujaybhowmick/OpenAlprDroidApp

## 1. Import OpenCV module
Go to **File->New->Import module** and provide a path to **unpacked_OpenCV_package/sdk/java**, where unpacked_OpenCV_package is the directory where you have unpacked the previously downloaded the last openCV version. The name of module is set automatically as **openCVLibraryXXX**, where XXX is your openCV version.

![](https://github.com/Montroigenc/openCL-object-detection-in-android-studio/blob/master/importOpencvDependency.png)
Click next and left all the settings in the next window as they come by default.

## 2. Add OpenCV dependency
To work with the OpenCV Android library, you have to add it to your app module as a dependency. To easily do this on Android Studio, click on **File -> Project Structure**.

When the project structure dialog opens, click on the app module and click on the **+** green sign in the right window side  and select **Module dependency**. When the choose modules dialog opens, select your **OpenCV library module** and click on **OK**.

Automatically you will return to the dependencies page, where you should see that the openCV module has been added as a dependency. Then click **OK** to finish this step.

Once you have added the dependency, the openCV dependency should have been added to your project structure, as seen in the following image.

![](https://github.com/Montroigenc/openCL-object-detection-in-android-studio/blob/master/importOpencvDependency2.png)

Now Android Studio might be showing that there are errors in the project, any worries, we will solve it in the next step.

## 3. Adapting the files
Go to Android Studio and look at your project structure, you should have a directory named **Gradle Scripts** (image below), open two files of this directory:
1. build.gradle(Module:app)
2. build.gradle(openCVLibraryXXX), where XXX is your openCV version (my version is 343).

![](https://github.com/Montroigenc/openCL-object-detection-in-android-studio/blob/master/compilesdkversion.png)

Copy compileSdkVersion from the first file to the second one. In my case compileSdkVersion is 28:

**compileSdkVersion 14 -> compileSdkVersion 28**

Copy the compileSdkVersion to all targetSdkVersion in **both files**.

**targetSdkVersion 14 -> targetSdkVersion 28**

In Android Studio, select **Build -> Make Project**. If any error occurr, it should be because you have forget to do any of the previous steps.

## 4. Add Native Libraries
On your file explorer, navigate to the folder where you extracted the content of the OpenCV Android library zip file. Open the **sdk/native** directory.
Copy the **libs** folder in the native folder over to your project app module main folder (Usually ProjectName/app/src/main) and rename the libs folder you just copied into your project to **jniLibs**.


## Native JNI implementation of OpenAlpr API is another project called [openalpr-droidapp-native](https://github.com/sujaybhowmick/openalpr-droidapp-native)
