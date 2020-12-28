package com.example.emotionkeyboardbuilder.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emotionkeyboardbuilder.R;
import com.example.emotionkeyboardbuilder.builder.EmotionKeyboardBuilder;
import com.example.emotionkeyboardbuilder.util.gif.GIfInTextView;

import java.util.ArrayList;
import java.util.List;

public class SendActivity extends AppCompatActivity {

    private static String TAG = "NoSendActivity";
    private LinearLayout llRoot, llMessage;
    private RecyclerView rvMessage;
    private EditText etMessage;
    private TextView tvEmotion, tvFlower;
    private RecyclerViewAdapter adapter;
    private View vTop;
    private FrameLayout flEmotion;
    private EmotionKeyboardBuilder emotionKeyboardBuilder;
    private List<View> evaluateList = new ArrayList<>();//评论视图列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        init();
    }
    private void init(){
        llRoot = findViewById(R.id.ll_root);
        llMessage = findViewById(R.id.ll_message);
        rvMessage = findViewById(R.id.rv_message);
        etMessage = findViewById(R.id.et_message);
        tvEmotion = findViewById(R.id.tv_emotion);
        tvFlower = findViewById(R.id.tv_flower);
        vTop = findViewById(R.id.v_top);
        flEmotion = findViewById(R.id.fl_emotion);
        evaluateList.add(llMessage);

        ViewGroup.LayoutParams layoutParams = vTop.getLayoutParams();
        layoutParams.height = getStatusBarHeight(this);
        vTop.setLayoutParams(layoutParams);

        rvMessage.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter(this);
        rvMessage.setAdapter(adapter);

        //表情布局
        EmotionKeyboardBuilder.Builder builder = new EmotionKeyboardBuilder.Builder(this);
        builder.bindType(1)
                .bindEditText(etMessage)
                .bindEmotionMap(GIfInTextView.EMOTION_CLASSIC_MAP)
                .bindMatcher("\\[\\d{1,2}\\|L]")
                .bindBottomView(flEmotion)
                .bindEmotionView(tvEmotion)
                .bindFlowerView(tvFlower)
                .bindRootView(llRoot)
                .setOnClickSendListener(new EmotionKeyboardBuilder.OnClickSend() {
                    @Override
                    public void send() {
                        if (etMessage.getText().toString().trim().length() != 0){
                            adapter.addMessage(etMessage.getText().toString());
                            etMessage.setText("");
                            emotionKeyboardBuilder.hideEmotion();
                            emotionKeyboardBuilder.hideKeyboard();
                        }else {
                            Toast.makeText(SendActivity.this, "不能发送空内容哦", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setOnClickFlowerListener(new EmotionKeyboardBuilder.OnClickFlower() {
                    @Override
                    public void flower() {
                        //这里本来是鲜花表情，可以根据需求修改
                        adapter.addMessage("[0|L]");
                    }
                })
                .build();
        emotionKeyboardBuilder = new EmotionKeyboardBuilder(builder);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        hideInputWhenTouchOtherView(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (emotionKeyboardBuilder != null){
            emotionKeyboardBuilder.updateViewMode();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (emotionKeyboardBuilder != null){
            emotionKeyboardBuilder.updateViewMode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (emotionKeyboardBuilder != null){
            emotionKeyboardBuilder.updateViewMode();
        }
    }

    /**
     * 触摸发表评价的编辑区域以外的区域
     */
    private void hideInputWhenTouchOtherView(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (evaluateList != null && !evaluateList.isEmpty()) {
                Log.e(TAG, "表情布局列表长度：" + evaluateList.size());
                for (int i = 0; i < evaluateList.size(); i++) {
                    if (isTouchingView(evaluateList.get(i), ev)) {
                        Log.e(TAG, "点击表情布局中的第" + evaluateList.get(i) + "项，不切换菜单");
                        return;
                    }
                }
            }
            View view = this.getCurrentFocus();
            if (isShouldHideInput(view, ev)) {
                InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    Log.e(TAG, "点击表情布局以外的部分，隐藏键盘");
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                emotionKeyboardBuilder.hideEmotion();
            }
        }
    }

    private boolean isShouldHideInput(View view, MotionEvent event) {
        if ((view instanceof EditText)) {
            return !isTouchingView(view, event);
        }
        return false;
    }

    /**
     * 是否正在触摸view
     * @param view 需要判断的view
     * @param event 动作
     * @return 返回判断
     */
    private boolean isTouchingView(View view, MotionEvent event) {
        if (view == null || event == null) {
            return false;
        }
        int[] leftTop = {0, 0};
        view.getLocationInWindow(leftTop);
        int left = leftTop[0];
        int top = leftTop[1];
        int bottom = top + view.getHeight();
        int right = left + view.getWidth();
        return (event.getRawX() > left) && (event.getRawX() < right) && (event.getRawY() > top) && (event.getRawY() < bottom);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

        private Context context;
        private List<String> messageList = new ArrayList<>();

        public RecyclerViewAdapter(Context context) {
            this.context = context;
        }

        public void addMessage(String message){
            messageList.add(message);
            notifyDataSetChanged();
        }

        public void setMessageList(List<String> messageList) {
            this.messageList = messageList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String regexEmotion = "\\[\\d{1,2}\\|L\\]";
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(messageList.get(position));
            stringBuilder = GIfInTextView.textToGif(((ViewHolder) holder).tvItem, regexEmotion, stringBuilder, context);
            ((ViewHolder) holder).tvItem.setText(stringBuilder);
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvItem;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvItem = itemView.findViewById(R.id.tv_item);
            }
        }
    }
}