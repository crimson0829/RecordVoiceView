package com.crimson.record.view

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.crimson.record.R
import com.crimson.record.audio.RecordConfig
import com.crimson.record.bus.RxBus.Companion.get
import com.crimson.record.bus.RxBusMessage
import com.crimson.record.bus.RxCode
import com.crimson.record.bus.RxDisposable.add
import com.crimson.record.bus.RxDisposable.remove
import com.crimson.record.data.RecordFile
import com.crimson.record.data.RecordNodeData
import com.crimson.record.data.RecordTime
import com.crimson.record.data.deleteFile
import com.crimson.record.player.AudioPlayer
import com.crimson.record.util.RxRecordTime.Companion.recordTime
import com.crimson.record.util.SoftKeyBoardListener.Companion.setListener
import com.crimson.record.util.SoftKeyBoardListener.OnSoftKeyBoardChangeListener
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

/**
 * @author crimson
 * @date 2019-09-05
 * 录音控件
 * 录音数据存储在节点对象[RecordNodeData]
 */
class RecordVoiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    //父布局
    private var mParent: ViewParent? = null
    //计算宽度
    private var widthMeasureSpec = 0
    //填充屏幕后的原始高度
    private var originHeight = 0
    //声音模式 1：录音 2：播放
    var voiceMode = 0
    //圆点标签
    //半径
    var thumbRadius = 0
    //颜色
    var thumbColor = 0
    //中心坐标
    var mCenterX = 0
    //时间距离
    var mTimeDistance = 0
    //速率  1000ms内重绘的速率，越小速度越快 如速率为100 则重绘1000/100=10次
    var speed = 0
    //初始左偏移量
    var originLeft = 0
    //进度线颜色
    var lineColor = 0
    //进度线宽度
    var lineWidth = 0
    //虚线颜色
    var lineDashColor = 0
    //虚线宽度
    var lineDashWidth = 0
    //时间颜色
    var timeColor = 0
    //时间大小
    var timeSize = 0
    //时间是否加粗
    var timeBold = false
    //正在识别颜色
    var recognizeColor = 0
    //正在识别大小
    var recognizeSize = 0
    //正在识别是否加粗
    var rercognizeBold = false
    //正在识别文字
    var recognizeText = ""
    //识别文字背景
    var recognizeTextBg = 0
    //节点圆颜色
    var circleColor = 0
    //节点圆外圈大小
    var circleWidth = 0
    //节点圆半径
    var circleRadius = 0
    //节点圆是否填充
    var circleFull = false
    //内圆颜色
    var innerCircleColor = 0
    //内容颜色
    var contentColor = 0
    //内容大小
    var contentSize = 0
    //翻译内容padding
    var concentPadding = 30
    //最小节点高度
    var minNodeHeight = 150
    //录音目录
    var recordDir: String? = null
    //录音文件名称
    var recordFileName: String? = null
    //录音文件设置
    var recordConfig: RecordConfig? = null
    //虚线路径
    private var mPath = Path()
    //画笔
    private var mThumbPaint = Paint()
    private var mLinePaint = Paint()
    private var mDashLinePaint = Paint()
    private var mTimePaint = Paint()
    private var mCirclePaint = Paint()
    private var mInnerCirclePaint = Paint()
    private var mContentPaint = TextPaint()
    private var mRecognizingPaint = Paint()
    private var mRecognizeBgPaint = Paint()
    //初始偏移量
    private val originY = 24
    //偏移量
    private var thumbY = originY
    //初始Y坐标
    private val startY = originY
    //图标等底部之后的滑动坐标
    private var overScrollY = 0
    private var mParentScrollY = 0
    private val overScrollYPreY = 200
    private val calculateHeightY = 500

    private var status = Status.IDLE

    //状态 0：初始状态 1：录音状态 2:录音暂定状态 3：播放状态 4：播放暂停状态,5.播放空闲状态 6.编辑内容状态
    enum class Status {
        IDLE, RECORDING, RECORD_PAUSE, PLAYING, PLAY_PAUSE, PLAY_IDLE, EDIT_CONTENT
    }

    //节点容器
    private var nodes = arrayListOf<RecordNodeData>()
    //当前节点
    private var currentNode = 0
    //是否需创建新的节点
    private var needNewNode = false
    //rx sub
    private var mRecSub: Disposable? = null
    private var mTimeSub: Disposable? = null
    private var mClipSub: Disposable? = null
    private var mPcmDataSub: Disposable? = null
    /**
     * 录音接口
     */
    private var recorder: IRecorder? = null
    //分
    private var mini: Long = 0
    //秒
    private var seco: Long = 0
    //播放状态进度线条绘制的高度
    private var lineDrawHeight = 0
    //播放器
    private var mPlayer: AudioPlayer? = null
    //节点文字content sb 容器
    private val arrays = SparseArray<StringBuilder?>()
    //临时转化文字
    private var psg: String? = null
    //是否可以点击和移动进度
    private var canMoveIcon = false
    //是否可以编辑内容
    private var canEditContent = false
    //前一个编辑框索引
    private var prevEditIndex = -1
    private var mHandler = Handler()
    private var mRunnable = Runnable {
        thumbY++
        if (voiceMode == 1) { //录音模式
            startRecord()
        } else { //播放模式
            if (thumbY < lineDrawHeight) { //播放
                startPlay()
            } else { //暂停
                pausePlay()
            }
        }
        if (status == Status.RECORDING && thumbY > height - overScrollYPreY) {
            scrollTo(0, overScrollY)
            overScrollY++
            if (nodes[currentNode].distance <= overScrollY + 20) {
                pauseRecord()
                startRecord()
            }
        } else {
            invalidate()
        }
    }

    init {
        initAttrs(context, attrs)
        init()
        initRxBus()
    }

    /**
     * 初始化属性
     *
     * @param context
     * @param attrs
     */
    private fun initAttrs(context: Context, attrs: AttributeSet?) {

        context.obtainStyledAttributes(attrs, R.styleable.RecordVoiceView)
            .apply {
                thumbRadius = getInt(R.styleable.RecordVoiceView_rvv_thumb_radius, dip2px(5))
                thumbColor =
                    getColor(
                        R.styleable.RecordVoiceView_rvv_thumb_color,
                        Color.parseColor("#14c691")
                    )
                voiceMode = getInt(R.styleable.RecordVoiceView_rvv_voice_mode, 1)
                speed = getInt(R.styleable.RecordVoiceView_rvv_speed, 80)
                originLeft = getInt(R.styleable.RecordVoiceView_rvv_line_leftPadding, dip2px(70))
                lineWidth = getInt(R.styleable.RecordVoiceView_rvv_line_width, dip2px(3))
                lineColor =
                    getColor(
                        R.styleable.RecordVoiceView_rvv_line_color,
                        Color.parseColor("#14c691")
                    )
                lineDashColor =
                    getColor(
                        R.styleable.RecordVoiceView_rvv_line_dash_color,
                        Color.parseColor("#999999")
                    )
                lineDashWidth = getInt(R.styleable.RecordVoiceView_rvv_line_dash_width, dip2px(2))
                timeColor =
                    getColor(
                        R.styleable.RecordVoiceView_rvv_node_time_color,
                        Color.parseColor("#666666")
                    )
                timeSize = getInt(R.styleable.RecordVoiceView_rvv_node_time_size, dip2px(15))
                timeBold = getBoolean(R.styleable.RecordVoiceView_rvv_node_time_bold, false)
                recognizeColor = getColor(
                    R.styleable.RecordVoiceView_rvv_recognize_color,
                    Color.parseColor("#999999")
                )
                recognizeSize = getInt(R.styleable.RecordVoiceView_rvv_recognize_size, dip2px(14))
                rercognizeBold = getBoolean(R.styleable.RecordVoiceView_rvv_recognize_bold, true)
                recognizeTextBg = getColor(
                    R.styleable.RecordVoiceView_rvv_recognize_text_bg,
                    Color.parseColor("#f5f5f5")
                )
                circleColor =
                    getColor(
                        R.styleable.RecordVoiceView_rvv_circle_color,
                        Color.parseColor("#14c691")
                    )
                circleWidth = getInt(R.styleable.RecordVoiceView_rvv_circle_width, dip2px(2))
                circleRadius = getInt(R.styleable.RecordVoiceView_rvv_circle_radius, dip2px(4))
                circleFull = getBoolean(R.styleable.RecordVoiceView_rvv_circle_full, false)
                innerCircleColor =
                    getColor(R.styleable.RecordVoiceView_rvv_innerCircle_color, Color.WHITE)
                contentColor = getColor(
                    R.styleable.RecordVoiceView_rvv_content_color,
                    Color.parseColor("#333333")
                )
                contentSize = getInt(R.styleable.RecordVoiceView_rvv_content_size, dip2px(15))


                recycle()
            }

    }

    /**
     * 初始化
     */
    private fun init() {
        initPaint()
        initVoiceMode()
    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {

        mCenterX = originLeft + thumbRadius
        mThumbPaint.color = thumbColor
        mLinePaint.color = lineColor

        mDashLinePaint.run {
            color = lineDashColor
            style = Paint.Style.STROKE
            strokeWidth = lineDashWidth.toFloat()
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        mTimePaint.run {
            color = timeColor
            style = Paint.Style.FILL
            isAntiAlias = true
            textSize = timeSize.toFloat()
            isFakeBoldText = timeBold
            textAlign = Paint.Align.CENTER
        }

        mTimeDistance = startY + thumbRadius

        mRecognizingPaint.run {
            color = recognizeColor
            style = Paint.Style.FILL
            isAntiAlias = true
            textSize = recognizeSize.toFloat()
            isFakeBoldText = rercognizeBold
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        mRecognizeBgPaint.run {
            color = recognizeTextBg
        }



        recognizeText = context.getString(R.string.recording)

        mCirclePaint.run {
            color = circleColor
            style = if (circleFull) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = circleWidth.toFloat()
            isAntiAlias = true

        }

        mInnerCirclePaint.run {
            color = innerCircleColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        mContentPaint.run {
            color = contentColor
            style = Paint.Style.FILL
            isAntiAlias = true
            textSize = contentSize.toFloat()
            textAlign = Paint.Align.LEFT
        }

        setBackgroundColor(ContextCompat.getColor(context,android.R.color.white))

    }

    /**
     * 根据不同录音模式初始化
     */
    private fun initVoiceMode() {
        if (voiceMode == 1) { //设置图层类型，不要开启硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            //录音模式
            nodes.add(RecordNodeData(0, mTimeDistance, "00:00", ""))
            //翻译内容布局
            nodes[0].contentLayout = createContentLayout(0, "")
            //recorder实现
            recorder = RecorderImpl(context)
        } else { //播放模式。相当于处于暂停状态
            status = Status.PLAY_IDLE
            mPlayer = AudioPlayer()
            if (context is Activity) { //软键盘监听
                setListener(
                    (context as Activity),
                    object : OnSoftKeyBoardChangeListener {
                        override fun keyBoardShow(height: Int) {
                            if (mStatusCallBack != null) {
                                mStatusCallBack?.onKeyBoardShow(height)
                            }
                        }

                        override fun keyBoardHide(height: Int) {
                            if (mStatusCallBack != null) {
                                mStatusCallBack?.onKeyBoardHide(height)
                            }
                            removeCurrentEditText(mParent as ViewGroup?)
                        }
                    })
            }
        }
    }

    /**
     * 语音识别内容回调注册
     */
    private fun initRxBus() {
        mRecSub = get()
            ?.toObservable(RxCode.ONLINE_RECORD_STATUS_CODE, RxBusMessage::class.java)
            ?.subscribe { (code, error) ->
                when (code) {
                    RxCode.ONLINE_RECORD_PARTIAL -> {
                        psg = error as String
                        if (mStatusCallBack != null) {
                            mStatusCallBack?.onVoiceRecordPartial(psg)
                        }
                        RecorderImpl.clipTime = 0
                        var sbp = arrays[currentNode]
                        var preMsg = ""
                        if (sbp == null) {
                            sbp = StringBuilder()
                            arrays.put(currentNode, sbp)
                        } else {
                            preMsg = sbp.toString()
                        }
                        nodes[currentNode].content = preMsg + psg
                        nodes[currentNode].contentLayout =
                            createContentLayout(currentNode, nodes[currentNode].content)
                        calculateContentHeight()
                        if (status != Status.RECORDING) {
                            postInvalidate()
                        }
                    }
                    RxCode.ONLINE_RECORD_FINISHED -> {
                        val sb: StringBuilder?
                        if (arrays[currentNode] == null) {
                            sb = StringBuilder()
                            arrays.put(currentNode, sb)
                        } else {
                            sb = arrays[currentNode]
                        }
                        val msg = error as String
                        if (mStatusCallBack != null) {
                            mStatusCallBack?.onVoiceRecordFinished(msg)
                        }
                        RecorderImpl.clipTime = 0
                        sb?.append(msg)
                        val content = nodes[currentNode].content
                        if (psg != null) {
                            nodes[currentNode].content = content.replace(psg!!, "") + msg
                        }
                        nodes[currentNode].contentLayout =
                            createContentLayout(currentNode, nodes[currentNode].content)
                        calculateContentHeight()
                        if (status != Status.RECORDING) {
                            postInvalidate()
                        }
                    }
                    RxCode.ONLINE_RECORD_ERROR -> {
                        if (arrays[currentNode] != null) {
                            val sbe = arrays[currentNode]
                            sbe?.append(nodes[currentNode].content)
                        }
                        if (error is String) {
                            if (mStatusCallBack != null) {
                                mStatusCallBack?.onVoiceRecordError(Error(error))
                            }
                            RecorderImpl.clipTime = 0
                            //这里是实现了百度asr的错误码，如果是2001,代表的是网络错误，不做处理
                            //其他的错误码就重新开启
                            if ("2001" != error) {
                                if (recorder == null) {
                                    return@subscribe
                                }
                                recorder?.restartRecord()
                            }
                        } else if (error is Error) {
                            if (mStatusCallBack != null) {
                                mStatusCallBack?.onVoiceRecordError(error)
                            }
                        }
                    }
                }
            }
        mClipSub = get()
            ?.toObservable(RxCode.RECORD_CLIP_CODE, Integer::class.java)
            ?.subscribeOn(Schedulers.io())
            ?.subscribe {
                //这里切割会出现一种极端的情况：当正好需要切割节点的时候，按了暂停，这样这里就回调了两次这样就会报错了
                val sb = arrays[currentNode]
                if (sb != null && sb.toString().trim().isNotEmpty() || recorder?.recordMode() === IRecorder.RecordMode.LOCAL || status == Status.RECORD_PAUSE
                ) {
                    if (mStatusCallBack != null) {
                        mStatusCallBack?.onRecordClip()
                    }
                    //只有当断点的地方有识别内容，才断下一个点
                    needNewNode = true
                    //关闭当前node对象文件流
                    val recordFile = nodes[currentNode].recordFile
                    recordFile?.pcmToWav()
                }
            }
        /**
         *
         * 统一处理从online  或者local 过来的pcm数据
         * 存储为file文件，格式为Y_年月日时分秒.wav
         * 存入当前节点对象的file current file
         */
        mPcmDataSub = get()
            ?.toObservable(RxCode.POST_PCM_DATA, ByteArray::class.java)
            ?.subscribeOn(Schedulers.io())
            ?.subscribe { data: ByteArray? ->
                if (status == Status.RECORD_PAUSE) {
                    return@subscribe
                }
                if (nodes.size - 1 < currentNode) { //会出现一种情况，还没有生成节点
                    return@subscribe
                }
                if (mStatusCallBack != null) {
                    mStatusCallBack?.onPcmDataResult(data)
                }
                val node = nodes[currentNode]
                if (node.recordFile == null) {
                    node.recordFile = createRecordFile()
                }
                val recordFile = node.recordFile
                if (recordFile != null && !recordFile.isClosed) {
                    recordFile.writeData(data)
                }
            }
        mTimeSub = get()
            ?.toObservable(RxCode.RECORD_TIME_CODE, RecordTime::class.java)
            ?.subscribe { (min, sec) ->
                mini = min
                seco = sec
            }
        add(mRecSub)
        add(mPcmDataSub)
        add(mClipSub)
        add(mTimeSub)
    }

    /**
     * 设置联网状态下录音实现类
     */
    fun setOnlineRecord(onlineRecorderImpl: IRecorder.IOnlineRecorder) {

        if (recorder is RecorderImpl) {
            (recorder as RecorderImpl).onlineRecorder(onlineRecorderImpl)
        }
    }

    /**
     * 设置有网条件下切割时间间隔，当为0时不自动切割节点
     */
    fun setOnlineClipTime(time: Int) {
        if (recorder is RecorderImpl) {
            (recorder as RecorderImpl).onlineClipTime(time)
        }
    }

    /**
     * 设置本地条件下切割时间，当为0时不自动切割节点
     */
    fun setLocalClipTime(time: Int) {
        if (recorder is RecorderImpl) {
            (recorder as RecorderImpl).localClipTime(time)
        }
    }

    /**
     * 设置录音模式
     */
    fun setRecordMode(mode: IRecorder.RecordMode) {
        if (recorder is RecorderImpl) {
            (recorder as RecorderImpl).recordMode(mode)
        }
    }


    /**
     * 计算内容的高度
     */
    private fun calculateContentHeight() {
        val distance = nodes[currentNode].distance
        val height = nodes[currentNode].contentLayout?.height
        if (height!! > 50 && thumbY < distance + height) {
            //如果偏移量小于识别文字的高度，就将偏移量设置成该高度
            //计算新增的偏移量
            val offsetY = distance + height - thumbY
            //赋值新的thumbY
            thumbY = distance + height
            if (thumbY > getHeight() - overScrollYPreY) { //赋值scrollY
                overScrollY += offsetY
            }
        }
    }

    /**
     * 创建内容绘制布局
     *
     * @param node
     * @param content
     * @return
     */
    private fun createContentLayout(node: Int, content: String): StaticLayout { //置空
        if (nodes.size - 1 > node && nodes[node].contentLayout != null) {
            nodes[node].contentLayout = null
        }
        return StaticLayout(
            content,
            mContentPaint,
            windowWidth - thumbRadius * 2 - originLeft - concentPadding * 2,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
        )
    }

    /**
     * 新增节点
     */
    private fun addNewNode(): RecordNodeData {
        val min = if (mini < 10) "0$mini" else mini.toString()
        val sec = if (seco < 10) "0$seco" else seco.toString()
        val distance = nodes[currentNode].distance
        //设置偏移量
        //第一种情况，设置默认节点最小高度150
        if (thumbY < distance + minNodeHeight) {
            val offsetY = distance + minNodeHeight - thumbY
            thumbY = distance + minNodeHeight
            if (thumbY > height - overScrollYPreY) { //赋值scrollY
                overScrollY += offsetY
            }
        } else { //有识别内容的时候，加75
            val cl = nodes[currentNode].contentLayout
            if (cl != null) {
                val height = cl.height
                //第一种情况，设置最小间距
                if (thumbY < distance + height + minNodeHeight / 2) {
                    //如果偏移量小于识别文字的高度，就将偏移量设置成该高度
                    //计算新增的偏移量
                    val offsetY = distance + height + minNodeHeight / 2 - thumbY
                    //赋值新的thumbY
                    thumbY = distance + height + minNodeHeight / 2
                    if (thumbY > getHeight() - overScrollYPreY) { //赋值scrollY
                        overScrollY += offsetY
                    }
                }
            }
        }
        val node = RecordNodeData(++currentNode, thumbY, "$min:$sec", "")
        if (node.recordFile == null) {
            node.recordFile = createRecordFile()
        }
        nodes.add(node)
        return node
    }

    //    /**
//     * 总时间
//     *
//     * @return
//     */
    fun totalTime(): Int {
        return recordTime
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        if (parent is ViewGroup) {
            val width = (parent as ViewGroup).width
            val height = (parent as ViewGroup).height
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        this.widthMeasureSpec = widthMeasureSpec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(
        changed: Boolean, l: Int, t: Int, r: Int, b: Int
    ) {
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //初始状态
        //默认虚线
        mPath.reset()
        mPath.moveTo(mCenterX.toFloat(), startY - 8.toFloat())
        mPath.lineTo(mCenterX.toFloat(), height + overScrollY.toFloat())
        canvas.drawPath(mPath, mDashLinePaint)
        //默认初始时间
        if (nodes.isNotEmpty()) {
            canvas.drawText(
                nodes[0].time,
                originLeft / 2.toFloat(),
                nodes[0].distance.toFloat(),
                mTimePaint
            )
        }
        canvas.drawCircle(mCenterX.toFloat(), thumbY.toFloat(), thumbRadius.toFloat(), mThumbPaint)
        if (status != Status.IDLE) {
            //绘制状态
            //画进度
            canvas.drawRect(
                mCenterX - lineWidth / 2.toFloat(),
                startY.toFloat(),
                mCenterX + lineWidth / 2.toFloat(),
                (if (voiceMode == 1) thumbY else lineDrawHeight) + thumbRadius.toFloat(),
                mLinePaint
            )
            //画初始节点
            canvas.drawCircle(
                mCenterX.toFloat(),
                startY.toFloat(),
                circleRadius.toFloat(),
                mCirclePaint
            )
            if (!circleFull) {
                canvas.drawCircle(
                    mCenterX.toFloat(),
                    startY.toFloat(),
                    circleRadius - 3.toFloat(),
                    mInnerCirclePaint
                )
            }
            //根据节点容器长度，画节点跟时间
            for (i in 1 until nodes.size) {
                val (_, distance, time) = nodes[i]
                //时间
                canvas.drawText(
                    time,
                    originLeft / 2.toFloat(),
                    distance + mTimeDistance.toFloat(),
                    mTimePaint
                )
                //节点
                canvas.drawCircle(
                    mCenterX.toFloat(),
                    distance.toFloat() + mTimeDistance.toFloat() / 2,
                    circleRadius.toFloat(),
                    mCirclePaint
                )
                if (!circleFull) {
                    canvas.drawCircle(
                        mCenterX.toFloat(),
                        distance + mTimeDistance / 2.toFloat(),
                        circleRadius - 3.toFloat(),
                        mInnerCirclePaint
                    )
                }
            }
            //图标
            canvas.drawCircle(
                mCenterX.toFloat(),
                thumbY.toFloat(),
                thumbRadius.toFloat(),
                mThumbPaint
            )
            if (status == Status.RECORDING && recorder?.recordMode() === IRecorder.RecordMode.ONLINE) { //展示正在识别
                val rectF = RectF(
                    (originLeft + thumbRadius * 2 + concentPadding).toFloat(),
                    (thumbY + concentPadding).toFloat(),
                    (originLeft + thumbRadius * 2 + concentPadding + dip2px(100)).toFloat(),
                    (thumbY + concentPadding + dip2px(35)).toFloat()
                )
                canvas.drawRoundRect(rectF, 50f, 50f, mRecognizeBgPaint)
                //计算baseline
                val fontMetrics = mRecognizingPaint.fontMetrics
                val distance =
                    (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
                val baseline = rectF.centerY() + distance
                canvas.drawText(recognizeText, rectF.centerX(), baseline, mRecognizingPaint)
            }
            // 根据节点长度画翻译内容
            for (i in nodes.indices) {
                canvas.save()
                if (i == 0) {
                    val dy = nodes[i].distance - mTimeDistance
                    canvas.translate(
                        originLeft + thumbRadius * 2 + concentPadding.toFloat(),
                        if (dy < 0) 0f else dy.toFloat()
                    )
                } else {
                    canvas.translate(
                        originLeft + thumbRadius * 2 + concentPadding.toFloat(),
                        nodes[i].distance.toFloat()
                    )
                }
                var contentLayout = nodes[i].contentLayout
                if (contentLayout == null) {
                    contentLayout = createContentLayout(i, nodes[i].content)
                }
                contentLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (voiceMode != 2) {
            return false
        }
        val x = event.x
        var y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                canMoveIcon =
                    x < originLeft + thumbRadius * 2 + concentPadding && y < lineDrawHeight
                canEditContent =
                    x > originLeft + thumbRadius * 2 + concentPadding && y < lineDrawHeight
                if (canMoveIcon) { //如果可移动，请求父控件不拦截事件
                    thumbY = y.toInt()
                    val parent = parent
                    (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
                }
                removeCurrentEditText(mParent as ViewGroup?)
            }
            MotionEvent.ACTION_MOVE -> {
                if (canMoveIcon) {
                    val startY = 10
                    if (y < startY) {
                        y = startY.toFloat()
                    }
                    if (y > lineDrawHeight) {
                        y = lineDrawHeight.toFloat()
                    }
                    thumbY = y.toInt()
                    postInvalidate()
                } else {
                    return super.onTouchEvent(event)
                }
                if (canMoveIcon) { //直接播放
                    if (thumbY != lineDrawHeight) {
                        val positon = calculateCurrentPlaySeekPosition()
                        val path = nodes[currentNode].recordFile?.file?.absolutePath
                        if (mPlayer != null) {
                            mPlayer?.play(path, positon * 1000)
                        }
                        if (!isPlaying) {
                            startPlay()
                        }
                    }
                    if (mStatusCallBack != null) {
                        mStatusCallBack?.onStatusChanged(status)
                    }
                } else { //在move的区域外点击，弹出并编辑框
                    if (voiceMode == 2 && canEditContent) {
                        addEditText(y.toInt())
                    }
                }
            }
            MotionEvent.ACTION_UP -> if (canMoveIcon) {
                if (thumbY != lineDrawHeight) {
                    val positon = calculateCurrentPlaySeekPosition()
                    val path = nodes[currentNode].recordFile?.file?.absolutePath
                    if (mPlayer != null) {
                        mPlayer?.play(path, positon * 1000)
                    }
                    if (!isPlaying) {
                        startPlay()
                    }
                }
                if (mStatusCallBack != null) {
                    mStatusCallBack?.onStatusChanged(status)
                }
            } else {
                if (voiceMode == 2 && canEditContent) {
                    addEditText(y.toInt())
                }
            }
        }
        return true
    }

    /**
     * 添加编辑框
     *
     * @param y
     */
    private fun addEditText(y: Int) {
        mParent = parent.parent
        if (mParent is RelativeLayout) {
            val nodeIndex = calculateCurrentNodeIndex(y)
            if (nodes.size - 1 >= nodeIndex) {
                removeCurrentEditText(mParent as ViewGroup?)
                val node = nodes[nodeIndex]
                val child = EditText(context)
                //设置lp
                val width = windowWidth - thumbRadius * 2 - originLeft
                val lp =
                    RelativeLayout.LayoutParams(width, RelativeLayout.LayoutParams.WRAP_CONTENT)
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                val parent = parent
                if (parent is NestedScrollView) {
                    mParentScrollY = parent.scrollY
                    if (nodeIndex == 0) {
                        val topMargin = node.distance - mTimeDistance - 5
                        lp.topMargin = if (topMargin < 0) 0 else topMargin
                    } else {
                        lp.topMargin = node.distance - 5 - mParentScrollY
                    }
                }
                child.layoutParams = lp
                //这是edittext属性
                child.setTextSize(TypedValue.COMPLEX_UNIT_PX,contentSize.toFloat())
                child.setTextColor(contentColor)
                child.setPadding(concentPadding, 0, concentPadding, 0)
                child.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.white
                    )
                )
                child.setText(node.content)
                child.gravity = Gravity.TOP
                (mParent as RelativeLayout).addView(child)
                node.contentLayout = createContentLayout(nodeIndex, "")
                node.content = ""
                invalidate()
                child.requestFocus()
                hideOrShowSoftInput()
                node.editText = child
                //赋值
                prevEditIndex = nodeIndex
                child.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    val height = v.height
                    //剩余的就是多个节点的时候
                    //计算节点的高度，如果et的高度大于节点的高度，就循环赋值计算
                    if (nodes.size == 1) { //只有一个节点的时候，只要height大于总进度长度就赋值重绘
                        if (height > lineDrawHeight) {
                            lineDrawHeight = height + mTimeDistance + minNodeHeight / 2
                            invalidate()
                            //改变了进度高度，要重新计算速率重新计算speed
                        }
                    } else if (nodeIndex == nodes.size - 1) { //多个节点编辑最后一个节点的时候，计算最后一个节点的进度高度
                        if (height > lineDrawHeight - nodes[nodeIndex].distance) {
                            lineDrawHeight =
                                nodes[nodeIndex].distance + height + mTimeDistance + minNodeHeight / 2
                            invalidate()
                        }
                    } else {
                        val dis0 = nodes[nodeIndex].distance
                        val dis1 = nodes[nodeIndex + 1].distance
                        val nodeHeight = dis1 - dis0
                        if (height > minNodeHeight && height < nodeHeight + minNodeHeight / 2) { //多出来的高度
                            val moreHeight = height - nodeHeight + minNodeHeight / 2
                            for (i in nodeIndex + 1 until nodes.size) {
                                nodes[i].distance =
                                    nodes[i].distance + moreHeight + mTimeDistance
                            }
                            lineDrawHeight += moreHeight + mTimeDistance
                            invalidate()
                        }
                    }
                    calculateHeight()
                    status = Status.EDIT_CONTENT
                    if (mStatusCallBack != null) {
                        mStatusCallBack?.onStatusChanged(status)
                    }
                }
            }
        }
    }

    /**
     * 移除当前添加的edittext
     *
     * @param parent
     */
    private fun removeCurrentEditText(parent: ViewGroup?) {
        if (prevEditIndex != -1) { //移除当前的edittext
            val pNode = nodes[prevEditIndex]
            val et = pNode.editText
            if (et != null) {
                et.clearFocus()
                pNode.content = et.text.toString()
                pNode.contentLayout = createContentLayout(prevEditIndex, pNode.content)
                invalidate()
                parent?.removeView(et)
                pNode.editText = null
                prevEditIndex = -1
            }
            calculateHeight()
        }
    }

    /**
     * 开始录音
     */
    fun startRecord() {
        if (currentNode == 0) { //第一次点击开始的时候，需创建file
            val node = nodes[currentNode]
            if (node.recordFile == null) {
                node.recordFile = createRecordFile()
            }
        }
        if (needNewNode) {
            needNewNode = false
            //新建node节点
            val node = addNewNode()
            if (mStatusCallBack != null) {
                mStatusCallBack?.onCreateNewNode(node)
            }
        }
        mHandler.postDelayed(mRunnable, speed.toLong())
        if (recorder != null && recorder?.recordStatus() !== IRecorder.RecordStatus.RECORDING) {
            recorder?.startRecord()
        }
        status = Status.RECORDING

        mStatusCallBack?.onStatusChanged(status)

    }


    @Synchronized
    private fun createRecordFile(): RecordFile {
        val recordFile = RecordFile(
            File(
                recordDir ?: context.filesDir.absolutePath,
                recordFileName ?: "rvv_${System.currentTimeMillis()}.pcm"
            ), recordConfig ?: RecordConfig()
        )
        mStatusCallBack?.onCreateRecordFile(recordFile)
        return recordFile
    }

    //是否正在录音
    val isRecording: Boolean
        get() = status == Status.RECORDING

    /**
     * 暂停录音
     */
    fun pauseRecord() {
        status = Status.RECORD_PAUSE
        mHandler.removeCallbacks(mRunnable)
        if (recorder != null) {
            recorder?.pauseRecord()
        }
        //生成一个节点
        get()?.post(RxCode.RECORD_CLIP_CODE, 0)
        if (mStatusCallBack != null) {
            mStatusCallBack?.onStatusChanged(status)
        }

    }

    /**
     * 完成录音，返回节点容器
     */
    fun completeRecord(): List<RecordNodeData>? {
        if (voiceMode == 1) {
            if (isRecording) {
                pauseRecord()
            }
            nodes[currentNode].recordFile?.checkFileConvert()

        } else {
            removeCurrentEditText(mParent as ViewGroup?)
        }
        return nodes
    }

    /**
     * 在播放模式下构建节点
     *
     * totalTime 总时长，用于计算绘制线条高度
     */
    fun buildPlayNode(datas: List<RecordNodeData>?, totalHeight: Int, totalTime: Int) {
        if (lineDrawHeight == 0) {
            lineDrawHeight = totalHeight
        }
        if (voiceMode == 1) { //如果是录音状态，就先清空node
            nodes.clear()
        }
        nodes.addAll(datas!!)
        if (voiceMode == 1) { //如果是录音状态，就滑动标签到最底部
            thumbY = lineDrawHeight
            //需要新节点
            currentNode = nodes.size - 1
            needNewNode = true
            //设置时间
            recordTime = totalTime
            seco = totalTime % 60.toLong()
            mini = totalTime / 60.toLong()
            post {
                //滚动
                if (lineDrawHeight > height - 100) { //如果录音的高度小于控件高度,计算超出高度的部分，滑动到改位置
                    overScrollY = lineDrawHeight - height + overScrollYPreY
                    scrollTo(0, overScrollY)
                }
            }
        }
        if (voiceMode == 2) {
            calculateHeight()
        }
        invalidate()
        status = Status.RECORD_PAUSE
    }

    //如果是播放模式，就重新计算控件的大小，
    //如果进度高于控件在屏幕的高度，就重新绘制高度
    private fun calculateHeight() {
        post {
            if (lineDrawHeight + calculateHeightY > (if (originHeight == 0) height else originHeight)) {
                if (originHeight == 0) {
                    originHeight = height
                }
                setMeasuredDimension(widthMeasureSpec, lineDrawHeight + calculateHeightY)
                layout(left, top, right, lineDrawHeight + calculateHeightY)
                if (mParentScrollY != 0) { //重新滑动到之前滑动的位置
                    val parent1 = parent
                    if (parent1 is NestedScrollView) {
                        parent1.postDelayed({
                            setMeasuredDimension(
                                widthMeasureSpec,
                                lineDrawHeight + calculateHeightY
                            )
                            layout(
                                left,
                                top,
                                right,
                                lineDrawHeight + calculateHeightY
                            )
                            val parent = parent
                            if (parent is NestedScrollView) {
                                parent.scrollTo(0, mParentScrollY)
                            }
                        }, 200)
                    }
                }
            }
        }
    }

    /**
     * 开始播放
     */
    fun startPlay() {
        //在到达底部之后开始重新播放播放
        if (thumbY == lineDrawHeight) {
            thumbY = originY
        }
        //根据当前节点偏移量计算出当前node索引
        val index = calculateCurrentNodeIndex()
        if (currentNode != index) {
            currentNode = index
        }
        //播放node file
        mPlayer?.play(nodes[index].recordFile?.file?.absolutePath)

        //计算播放速率
        //高度
        val height: Int
        height = if (nodes.size - 1 == index || nodes.size == 1) {
            //最后一个节点或者只有一个节点的时候
            lineDrawHeight - nodes[index].distance
        } else { //其他情况
            nodes[index + 1].distance - nodes[index].distance
        }
        //时间
        val duration = nodes[index].duration
        if (duration != null && !duration.isEmpty()) {
            speed = duration.toInt() * 1000 / height
        }
        mHandler.postDelayed(mRunnable, speed.toLong())
        status = Status.PLAYING
        if (mStatusCallBack != null) {
            mStatusCallBack?.onStatusChanged(status)
        }
    }

    /**
     * 是否有节点
     *
     * @return
     */
    fun hasNode(): Boolean {
        return nodes.size != 0
    }

    /**
     * 获取录音线条总高度
     */
    fun totalHeight(): Int {
        return if (voiceMode == 1) thumbY else lineDrawHeight
    }

    /**
     * 计算当前的node索引
     *
     * @return
     */
    private fun calculateCurrentNodeIndex(y: Int = thumbY): Int {
        var index = 0
        for (i in 1 until nodes.size) {
            //第一个节点
            val disF = nodes[1].distance
            if (y >= originY && y < disF - thumbRadius) {
                break
            }
            //中间节点
            val dis0 = nodes[i - 1].distance
            val dis1 = nodes[i].distance
            if (y >= dis0 - thumbRadius && y < dis1 - thumbRadius) {
                index = i - 1
                break
            }
            //最后一个节点
            if (i + 1 == nodes.size) {
                val disL = nodes[i].distance - thumbRadius
                if (y >= disL) {
                    index = i
                    break
                }
            }
        }
        return index
    }

    /**
     * 计算当前播放的seek进度
     *
     * @return
     */
    private fun calculateCurrentPlaySeekPosition(): Int {
        val index = calculateCurrentNodeIndex()
        //时长
        var duration = nodes[index].duration?.toInt()
        if (duration == null) {
            duration = 0
        }
        val position: Int
        position = if (nodes.size == 1) { //只有一个节点
            (thumbY - originY) * duration / (lineDrawHeight - originY)
        } else {
            if (index == 0) { //第一个节点
                val dis0 = nodes[0].distance
                val dis1 = nodes[1].distance
                (thumbY - dis0) * duration / (dis1 - dis0)
            } else if (index == nodes.size - 1) { //最后一个节点
                val dis = nodes[nodes.size - 1].distance
                (thumbY - dis) * duration / (lineDrawHeight - dis)
            } else { //中间节点
                val dis0 = nodes[index].distance
                val dis1 = nodes[index + 1].distance
                (thumbY - dis0) * duration / (dis1 - dis0)
            }
        }
        return position
    }

    /**
     * 暂停播放
     */
    fun pausePlay() {
        mPlayer?.pause()
        mHandler.removeCallbacks(mRunnable)
        status = Status.PLAY_PAUSE
        if (mStatusCallBack != null) {
            mStatusCallBack?.onStatusChanged(status)
        }
    }

    /**
     * 是否处于播放状态
     *
     * @return
     */
    val isPlaying: Boolean
        get() = status == Status.PLAYING


    /**
     * 删除录音文件
     */
    fun deleteFiles() {
        for (i in nodes.indices) {
            val node = nodes[i]
            val recordFile = node.recordFile
            if (recordFile != null) {
                val file = recordFile.file
                deleteFile(file)
            }
        }
    }

    /**
     * 释放
     */
    fun release() {

        remove(mRecSub)
        remove(mPcmDataSub)
        remove(mClipSub)
        remove(mTimeSub)

        mHandler.removeCallbacksAndMessages(null)

        nodes.clear()
        recorder?.release()
        mPlayer?.stop()
        mPlayer = null
        mStatusCallBack = null

    }


    private var mStatusCallBack: ViewStatusCallBack? = null

    fun setCallBack(listener: CallBack.() -> Unit) {
        val callBack = CallBack()
        callBack.listener()
        this.mStatusCallBack = callBack
    }


    private fun dip2px(dpValue: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dpValue * density + 0.5f).toInt()
    }

    /**
     * 获取屏幕宽度
     *
     * @return
     */
    private val windowWidth: Int
        get() {
            val wm =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            wm.defaultDisplay.getMetrics(dm)
            return dm.widthPixels
        }

    /**
     * 隐藏或显示软键盘
     */
    private fun hideOrShowSoftInput() {
        val imm = context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.toggleSoftInput(
            InputMethodManager.SHOW_IMPLICIT,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

}

interface ViewStatusCallBack {
    //状态改变
    fun onStatusChanged(status: RecordVoiceView.Status?)

    //新增节点
    fun onCreateNewNode(node: RecordNodeData?)

    //部分语音识别
    fun onVoiceRecordPartial(msg: String?)

    //一段话语音识别结束
    fun onVoiceRecordFinished(msg: String?)

    //语音识别错误
    fun onVoiceRecordError(error: Error?)

    //切割节点回调
    fun onRecordClip()

    //当创建录音时候
    fun onCreateRecordFile(file: RecordFile)

    //录音 pcm 字节流回调
    fun onPcmDataResult(data: ByteArray?)

    //键盘展示
    fun onKeyBoardShow(height: Int)

    //键盘隐藏
    fun onKeyBoardHide(height: Int)
}


class CallBack : ViewStatusCallBack {

    private var status: (RecordVoiceView.Status?) -> Unit = {}
    private var newNode: (RecordNodeData?) -> Unit = {}
    private var recordPartial: (String?) -> Unit = {}
    private var recordFinished: (String?) -> Unit = {}
    private var recordError: (Error?) -> Unit = {}
    private var recordClip: () -> Unit = {}
    private var createRecordFile: (RecordFile?) -> Unit = {}
    private var pcmDataResult: (ByteArray?) -> Unit = {}
    private var keyBoardShow: (Int?) -> Unit = {}
    private var keyBoardHide: (Int?) -> Unit = {}

    /**
     * 状态改变回调
     */
    fun statusChangedCallBack(callback: (RecordVoiceView.Status?) -> Unit) {
        status = callback
    }

    /**
     * 创建新节点回调
     */
    fun createNewNodeCallBack(callback: (RecordNodeData?) -> Unit) {
        newNode = callback
    }

    /**
     * 部分语音转换回调
     */
    fun voiceToTextPartialCallBack(callback: (String?) -> Unit) {
        recordPartial = callback
    }

    /**
     * 一段语音结束转换回调
     */
    fun voiceToTextFinishedCallBack(callback: (String?) -> Unit) {
        recordFinished = callback
    }

    /**
     * 语音转换错误回调
     */
    fun voiceToTextErrorCallBack(callback: (Error?) -> Unit) {
        recordError = callback
    }

    /**
     * 语音切割时回调
     */
    fun recordClipCallBack(callback: () -> Unit) {
        recordClip = callback
    }

    /**
     * 创建录音文件时回调
     */
    fun createRecordFileCallBack(callback: (RecordFile?) -> Unit) {
        createRecordFile = callback
    }

    /**
     * pcm录音数据回调
     */
    fun pcmDataResultCallBack(callback: (ByteArray?) -> Unit) {
        pcmDataResult = callback
    }

    /**
     * 键盘显示回调
     */
    fun keyBoardShowCallBack(callback: (Int?) -> Unit) {
        keyBoardShow = callback
    }

    /**
     * 键盘隐藏回调
     */
    fun keyBoardHideCallBack(callback: (Int?) -> Unit) {
        keyBoardHide = callback
    }

    override fun onStatusChanged(status: RecordVoiceView.Status?) {
        this.status.invoke(status)
    }


    override fun onCreateNewNode(node: RecordNodeData?) {
        newNode.invoke(node)
    }

    override fun onVoiceRecordPartial(msg: String?) {
        recordPartial.invoke(msg)
    }

    override fun onVoiceRecordFinished(msg: String?) {
        recordFinished.invoke(msg)
    }

    override fun onVoiceRecordError(error: Error?) {
        recordError.invoke(error)
    }

    override fun onRecordClip() {
        recordClip.invoke()
    }

    override fun onCreateRecordFile(file: RecordFile) {
        createRecordFile.invoke(file)
    }

    override fun onPcmDataResult(data: ByteArray?) {
        pcmDataResult.invoke(data)
    }

    override fun onKeyBoardShow(height: Int) {
        keyBoardShow.invoke(height)
    }

    override fun onKeyBoardHide(height: Int) {
        keyBoardHide.invoke(height)
    }

}



