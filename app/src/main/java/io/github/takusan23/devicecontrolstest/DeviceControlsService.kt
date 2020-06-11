package io.github.takusan23.devicecontrolstest

import android.app.PendingIntent
import android.content.Intent
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.*
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.reactivestreams.FlowAdapters
import java.util.concurrent.Flow
import java.util.function.Consumer

class DeviceControlsService : ControlsProviderService() {

    private lateinit var updatePublisher: ReplayProcessor<Control>

    val TOGGLE_BUTTON_ID = "toggle_button_id"
    val SLIDER_BUTTON_ID = "slider_button_id"

    /**
     * ユーザーにコントロール一覧を表示する際に使われる
     * この段階ではON/OFFなどの情報はわからないため、StatelessBuilderの方を使う。
     * */
    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {

        val intent = Intent(baseContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // まとめてコントローラーを追加するので配列に
        val controlList = mutableListOf<Control>()

        // ON/OFFサンプル。
        val toggleControl = Control.StatelessBuilder(TOGGLE_BUTTON_ID, pendingIntent)
            .setTitle("ON/OFFサンプル") // たいとる
            .setSubtitle("おすとON/OFFが切り替わります。") // サブタイトル
            .setDeviceType(DeviceTypes.TYPE_FAN) // 多分アイコンに使われてる？
            .build()
        // スライダーサンプル
        val sliderControl = Control.StatelessBuilder(SLIDER_BUTTON_ID, pendingIntent)
            .setTitle("スライダーサンプル") // たいとる
            .setSubtitle("スライダーです。") // サブタイトル
            .setDeviceType(DeviceTypes.TYPE_LIGHT) // 多分アイコンに使われてる？
            .build()

        controlList.add(toggleControl)
        controlList.add(sliderControl)

        // Reactive Streamsの知識が必要な模様。私にはないのでサンプルコピペする。
        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controlList))

    }

    /**
     * コントローラーをユーザーが操作したら呼ばれる。
     * setOnClickListener{ } みたいな感じ？
     * @param p0 デバイスID。StatelessBuilder()の第一引数
     * @param p1 BooleanAction（ON/OFF）やFloatAction（スライダー）など
     * @param p2
     * */
    override fun performControlAction(p0: String, p1: ControlAction, p2: Consumer<Int>) {
        // コントローラーを長押ししたときに表示するActivity
        val intent = Intent(baseContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // システムに処理中とおしえる
        p2.accept(ControlAction.RESPONSE_OK)
        // コントローラー分岐
        when (p0) {
            TOGGLE_BUTTON_ID -> {
                // ON/OFF
                // ToggleTemplate は BooleanAction
                if (p1 is BooleanAction) {
                    // ON/OFFボタン
                    val isOn = p1.newState
                    val message = if (isOn) "ONです" else "OFFです"
                    val toggle = ToggleTemplate("toggle_template", ControlButton(isOn, message))
                    // Control更新
                    val control = Control.StatefulBuilder(TOGGLE_BUTTON_ID, pendingIntent)
                        .setTitle("ON/OFFサンプル") // たいとる
                        .setSubtitle("おすとON/OFFが切り替わります。") // サブタイトル
                        .setDeviceType(DeviceTypes.TYPE_FAN) // 多分アイコンに使われてる？
                        .setStatus(Control.STATUS_OK) // 現在の状態
                        .setControlTemplate(toggle) // 今回はON/OFFボタン
                        .setStatusText(message)
                        .build()
                    updatePublisher.onNext(control)
                }
            }
            SLIDER_BUTTON_ID -> {
                // スライダー
                // RangeTemplate は FloatAction
                if (p1 is FloatAction) {
                    // 現在の値
                    val currentValue = p1.newValue
                    val slider = RangeTemplate("range_template", 1f, 10f, currentValue, 1f, "%.1f")
                    // Control更新
                    val control = Control.StatefulBuilder(SLIDER_BUTTON_ID, pendingIntent)
                        .setTitle("スライダーサンプル") // たいとる
                        .setSubtitle("スライダーです。") // サブタイトル
                        .setDeviceType(DeviceTypes.TYPE_LIGHT) // 多分アイコンに使われてる？
                        .setStatus(Control.STATUS_OK) // 現在の状態
                        .setControlTemplate(slider) // 今回はスライダー
                        .build()
                    updatePublisher.onNext(control)
                }
            }
        }
    }

    /**
     * ユーザーが選んだコントローラーを表示する際に来る
     * 電源ボタン長押しでもよばれる。
     * 「読み込んでいます」が永遠に表示される場合は多分deviceIdとかが違う気がする。
     * */
    override fun createPublisherFor(p0: MutableList<String>): Flow.Publisher<Control> {

        // コントローラーを長押ししたときに表示するActivity
        val intent = Intent(baseContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(baseContext, 12, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // 知識不足でわからん
        updatePublisher = ReplayProcessor.create()

        // 分岐
        if (p0.contains(TOGGLE_BUTTON_ID)) {
            // ON/OFF
            val toggle = ToggleTemplate("toggle_template", ControlButton(false, "OFFですねえ！"))
            // ここで作るControlは StatefulBuilder を使う。
            val control = Control.StatefulBuilder(TOGGLE_BUTTON_ID, pendingIntent)
                .setTitle("ON/OFFサンプル") // たいとる
                .setSubtitle("おすとON/OFFが切り替わります。") // サブタイトル
                .setDeviceType(DeviceTypes.TYPE_FAN) // 多分アイコンに使われてる？
                .setStatus(Control.STATUS_OK) // 現在の状態
                .setControlTemplate(toggle) // 今回はON/OFFボタン
                .build()
            updatePublisher.onNext(control)
        }
        if (p0.contains(SLIDER_BUTTON_ID)) {
            // スライダー
            val slider = RangeTemplate("range_template", 1f, 10f, 5f, 1f, null)
            // ここで作るControlは StatefulBuilder を使う。
            val control = Control.StatefulBuilder(SLIDER_BUTTON_ID, pendingIntent)
                .setTitle("スライダーサンプル") // たいとる
                .setSubtitle("スライダーです。") // サブタイトル
                .setDeviceType(DeviceTypes.TYPE_LIGHT) // 多分アイコンに使われてる？
                .setStatus(Control.STATUS_OK) // 現在の状態
                .setControlTemplate(slider) // 今回はスライダー
                .build()
            updatePublisher.onNext(control)
        }

        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

}