package com.crimson.record.bus

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject


/**
 * @author crimson
 * @date @date 2019-09-20
 * rx bus
 */
class RxBus private constructor() {

    companion object {
        @Volatile
        private var instance: RxBus? = null

        fun get(): RxBus? {
            if (instance == null) {
                synchronized(RxBus::class.java) {
                    if (instance == null) {
                        instance = RxBus()
                    }
                }
            }
            return instance
        }
    }

    private val bus: Subject<Any> = PublishSubject.create()

    fun send(o: Any) {
        bus.onNext(o)
    }

    fun toObservable(): Observable<Any> {
        return bus
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 被观察者
     *
     * @param eventType 事件类型
     * @param <T>
     * @return
    </T> */
    fun <T> toObservable(eventType: Class<T>?): Observable<T> {
        return bus.ofType(eventType)
    }

    /**
     * 提供了一个新的事件,根据code进行分发
     *
     * @param code 事件code
     * @param o
     */
    fun post(code: Int, o: Any?) {
        bus.onNext(RxBusMessage(code, o!!))
    }

    /**
     * 根据传递的code和 eventType 类型返回特定类型(eventType)的 被观察者
     * 对于注册了code为0，class为voidMessage的观察者，那么就接收不到code为0之外的voidMessage。
     *
     * @param code      事件code
     * @param eventType 事件类型
     * @param <T>
     * @return
    </T> */
    fun <T> toObservable(
        code: Int,
        eventType: Class<T>
    ): Observable<T> {
        return bus.ofType(RxBusMessage::class.java)
            .filter { (code1, obj) ->
                code1 == code && eventType.isInstance(
                    obj
                )
            }.map(RxBusMessage::obj).cast(eventType)
    }

    /**
     * 判断是否有订阅者
     */
    fun hasObservers(): Boolean {
        return bus.hasObservers()
    }


}

/**
 * @author crimson
 * @date @date 2019-09-20
 * rx bus 发送对象
 */
data class RxBusMessage(var code: Int, var obj: Any)