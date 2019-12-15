# RecordVoiceView

## 介绍

一个录音控件，将语音实时录入到本地，通过ASR实现,可将语音识别转换为文字。

RecordVoiceView -- 录音控件，支持语音的录制和播放

RecordNotification -- 录音通知栏


<br>

![RecordVoiceView](https://github.com/crimson0829/RecordVoiceView/blob/master/snapshot/shot1.jpg)

<br>



## 引入

单独引用录音库

```
dependencies {
            
    implementation 'com.github.crimson0829.RecordVoiceView:record_library:1.0'	
	       	              
}
	
```

全引用

```
dependencies {
                
    implementation "com.github.crimson0829:RecordVoiceView:1.0"
    //rx and okio
    implementation "io.reactivex.rxjava3:rxjava:3.0.0-RC5"
    implementation 'io.reactivex.rxjava3:rxandroid:3.0.0-SNAPSHOT'
    implementation "com.squareup.okio:okio:2.4.1"  
       
}
    	
```


## 使用

```
    <com.crimson.record.view.RecordVoiceView
        android:id="@+id/rv_view"
        android:layout_width="match_parent"
        app:rvv_voice_mode="record"
        android:layout_height="match_parent" />
```


```
     rv_view.apply {
     
               //设置有网状态下录音实现
               setOnlineRecord(BDOnlineRecordImpl(context))
               //设置有网切割时间间隔
               setOnlineClipTime(5)
               //设置本地切割时间间隔
               setLocalClipTime(15)
               //设置录音模式
               setRecordMode(IRecorder.RecordMode.ONLINE)
               //监听器设置
               setCallBack {
   
                   statusChangedCallBack {
   
                       println("status -> ${it.toString()}")
   
                   }
     
               }
           }
           
   //开始录音
   rv_view.startRecord()
    
   //暂停录音
   rv_view.pauseRecord()
    
   //完成录音
   val nodes = rv_view.completeRecord()
   
   //总时间
   val totalTime = rv_view.totalTime()
   
   //总高度
   val totalHeight = rv_view.totalHeight()
   
   //构建播放节点
   rv_view.buildPlayNode(nodes,height,time)
    
   //开始播放录音
   rv_view.startPlay()
   
   //暂停录音播放
   rv_view.pausePlay()
    
   //释放
   rv_view.release()

```


通知栏使用

```
    val notify by lazy {
        RecordNotification(this,this::class.java)
            .smallIcon()
            .largeIcon()
            .recordingTitle()
           .pauseTitle()
    }
    
    //展示
    notify.show()
    //暂停
    notify.pause()
    //释放
    notify.release()

```



# 属性


| 属性                   | 方法          | 定义                     |
|:---------------------|:------------|:-----------------------|
| rvv_voice_mode       | voiceMode   | 录音模式：record为录音,play为播放 |
| rvv_speed       | speed   | 指示器速率 越小速度越快 |
| rvv_thumb_radius     | thumbRadius | 指示器半径                  |
| rvv_thumb_color      | thumbColor  | 指示器颜色                  |
| rvv_line_leftPadding | originLeft       | 进度线左偏移量                  |
| rvv_line_width                | lineWidth       | 进度线宽度                  |
| rvv_line_color                | lineColor       | 进度线颜色                  |
| rvv_line_dash_color                | lineDashColor       | 指示虚线颜色                  |
| rvv_line_dash_width                | lineDashWidth       | 指示虚线宽度                  |
| rvv_node_time_color                | timeColor       | 节点时间颜色                  |
| rvv_node_time_bold                | timeBold       | 节点时间是否加粗                  |
| rvv_node_time_size                | timeSize       | 节点时间字体大小                  |
| rvv_recognize_color                | recognizeColor       | 正在识别提示字体颜色                  |
| rvv_recognize_bold                | recognizeBold       | 正在识别提示字体是否加粗                  |
| rvv_recognize_size                | recognizeSize       | 正在识别提示字体大小                |
| rvv_recognize_text_bg                | recognizeTextBg       | 正在识别提示字体背景颜色                  |
| rvv_circle_color                | circleColor       | 节点圆颜色                 |
| rvv_circle_width                | circleWidth       | 节点圆外部大小                |
| rvv_circle_radius                | circleRadius       | 节点圆半径                  |
| rvv_circle_full                | circleFull       | 节点圆是否填充                  |
| rvv_innerCircle_color                | innerCircleColor       | 节点内部圆颜色                  |
| rvv_content_color                | contentColor       | 识别内容颜色                  |
| rvv_content_size                | contentSize       | 识别内容字体大小                  |
|                 | recognizeText       | 设置'正在识别'文字                  |
|                 | contentPadding       | 识别文字padding值                  |
|                 | minNodeHeight       | 最小节点高度                  |
|                 | recordDir       | 录音文件夹路径                  |
|                 | recordConfig       | 录音文件参数配置                  |
|                 | setOnlineRecord()       | 设置有网模式语音识别实现，默认不实现              |
|                 | setOnlineClipTime()       | 设置有网模式节点切割时间间隔，默认5s                  |
|                 | setLocalClipTime()       | 设置本地模式节点切割时间间隔，默认15s                  |
|                 | setRecordMode()       | 设置录音模式                  |
               

## ASR语音识别转换为文字


使用百度ASR实现语音识别

```
dependencies {
                
    implementation 'com.github.crimson0829.RecordVoiceView:baidu_asr_library:1.0'
    	      
}
    	
```

提供了百度ASR的实现baidu_asr_library -- BDOnlineRecordImpl；

**在 AndroidManifest.xml 添加自己申请的 APP_ID、API_KEY、SECRET_KEY。**

[百度ASR申请地址](https://ai.baidu.com/tech/speech)

如果想要自己实现，请实现 IRecorder.IOnlineRecorder 接口，并自己实现方法，设置实现类  RecordVoiceView.setOnlineRecord() 。


## License

```
Copyright 2019 crimson
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


