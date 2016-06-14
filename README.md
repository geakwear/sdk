# Geakwear Developer SDK
为了方便第三方开发者迅速使用SDK来开发Geakwear应用，我们提供了以下联系方式来协助开发：  

**微博：果壳电子**  
**官网：[果壳开发者][igeak-dev]**  
**社区：[果壳极客团][igeak-10w]**  

## 非Android Wear开发者
我们建议您采用如下步骤：
* Step 1：运行sample文件夹里的示例程序来了解SDK提供的基本功能，如数据传输
* Step 2：阅读开发文档深入了解如何使用: [快速入门][getting-started]

## Android Wear开发者
Android Wear应用目前分为国际版和中国版，中国版的应用需要使用裁剪版的SDK（在文件夹android-wear-lib中可以找到）。
关于如何开发一个中国版的Android Wear应用，并将此应用移植到Geakwear平台，请参考Android Wear应用兼容文档：  
[AW应用兼容][gms-compat]

如果您想让您的应用同时兼容Android Wear（国际版或中国版）和Geakwear，请采用如下步骤

1. 添加Geakwear打包方式
  * 确保你的工程根目录的 `build.gradle` 文件中包含了 jcenter 代码库，或者 mavenCentral 代码库：

    ``` gradle
    repositories {
        jcenter()
    }
    ```
    或者：
    ``` gradle
    repositories {
        mavenCentral()
    }
    ```

  * 在工程根目录的 `build.gradle` 中添加对 Geakwear 打包插件的依赖：

    ``` gradle
    dependencies {
        classpath 'com.igeak.tools.build:igeak:1.0.4'
    }
    ```

  * 在 Mobile Module 的 `build.gradle` 中使用 Geakwear 打包插件：

    ``` gradle
    apply plugin: 'igeak'
    ```
    
  * 使用 release 方式打包
  * 更多应用打包详情，参考开发者文档中的[打包应用][pack-apps]

2. 如果你使用了GMS通讯，需要替换成 Geakwear Mobile Services：
  * 引入geakwear-api.jar，同时保留google-play-services.jar
  * 将代码中的Google Mobile Services (GMS) API替换为仅包名不同的Geak Mobile Services (GKMS) API，GoogleApiClient替换为GeakApiClient。在AndroidManifest.xml里面把`com.google.android.gms.wearable.BIND_LISTENER`替换为`com.igeak.android.wearable.BIND_LISTENER`
  * 在App启动时调用GeakApiManager.getInstance().adaptService(context)，该方法必须在任何可能的API调用操作前调用，它将会自动探测当前系统情况，选择底层是使用GKMS或GMS。如果想自己决定使用哪种API，可以通过调用GeakApiManager.getInstance().loadService(context, group)来指定使用Geakwear或Android Wear的API，以取代上面的adaptService方法。如果这两个方法都没有被调用，API会变成仅Geakwear系统能使用的方式。
  * 在AndroidManifest.xml中注册GMS Wearable Listener Service的代理服务：

    ```java
    <service android:name="com.igeak.android.wearable.WearableListenerServiceGoogleImpl">
      <intent-filter>
        <action android:name="com.google.android.gms.wearable.BIND_LISTENER" />
      </intent-filter>
    </service> 
    ```
* 重新编译打包

3. 更多兼容AW的详情，参考开发者文档中[AW应用兼容][gms-compat]

[igeak-dev]: http://developer.igeak.com/
[igeak-10w]: http://10w.igeak.com/
[getting-started]: /doc/getting-started.md
[gms-compat]: /doc/gms-compat.md
[pack-apps]: /doc/getting-started.md#打包应用
