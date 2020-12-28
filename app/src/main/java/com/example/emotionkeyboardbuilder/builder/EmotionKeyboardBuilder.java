package com.example.emotionkeyboardbuilder.builder;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


import com.bumptech.glide.Glide;
import com.example.emotionkeyboardbuilder.R;
import com.example.emotionkeyboardbuilder.util.DisplayUtils;
import com.example.emotionkeyboardbuilder.util.GlobalLayoutListener;
import com.example.emotionkeyboardbuilder.util.OnKeyboardChangedListener;
import com.example.emotionkeyboardbuilder.view.EmojiIndicatorView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @作者 yonghe Xie
 * @创建/修改日期 2020-12-01 13:48
 * @类名 EmotionKeyboardBuilder
 * @所在包 com\example\emotionkeyboardbuilder\builder\EmotionKeyboardBuilder.java
 * 表情面板构造者模式
 * 以下为使用方法：
 * 1.需要用到以下屏幕监听工具
 * {@link com.example.emotionkeyboardbuilder.util.DisplayMetricsHolder}
 * {@link com.example.emotionkeyboardbuilder.util.GlobalLayoutListener}
 * {@link com.example.emotionkeyboardbuilder.util.OnKeyboardChangedListener}
 * {@link com.example.emotionkeyboardbuilder.util.PixelUtil}
 * 2.build
 * {@link Builder#Builder(Activity)}传入activity
 * {@link Builder#bindType(int)}传入构建类型，0为不带发送按钮在末尾，1为带发送按钮在末尾，默认0
 * {@link Builder#bindEditText(EditText)}//传入编辑框
 * {@link Builder#bindEmotionMap(ArrayMap)}传入表情正则map
 * {@link Builder#bindMatcher(String)}传入正则表达式
 * {@link Builder#bindBottomView(ViewGroup)}//传入表情占位符
 * {@link Builder#bindEmotionView(View)}//传入表情开关
 * {@link Builder#bindRootView(ViewGroup)}//传入根布局
 * {@link Builder#bindFlowerView(View)}//项目功能需求，一键发送鲜花按钮，可修改
 * {@link Builder#setOnClickSendListener(OnClickSend)}//1类型的末尾发送监听
 * {@link Builder#setOnClickFlowerListener(OnClickFlower)}//项目功能需求，一键发送鲜花按钮监听，可修改
 * 3.需要在activity调用的方法
 * {@link EmotionKeyboardBuilder#updateViewMode()}//如果需要横屏隐藏键盘，则需要在onConfigurationChanged中调用
 */
public class EmotionKeyboardBuilder {
    private static String TAG = "EmotionKeyboardBuilder";
    private Activity context;
    private int type;//表情布局类型
    private EditText editText;//绑定的编辑框
    private ViewPager vpEmotion, vpAdd;//表情面板和加号面板
    private ViewGroup emotionParent;//装表情的总布局
    private View emotion;//表情按钮
    private View flower;//鲜花按钮
    private ViewGroup rootView;//根布局
    private OnClickSend onClickSend;
    private OnClickFlower onClickFlower;
    private int keyboardHeight = 0;//键盘高度
    private boolean isKeyboardShow = false;//键盘是否显示
    private boolean isEmotionShow = false;//表情面板是否在显示
    private InputMethodManager inputMethodManager;//键盘管理类
    public static ArrayMap<String, Integer> EMOTION_CLASSIC_MAP;
    public String matcher;
    private LinearLayout llEmotion;
    private List<RecyclerViewAdapter> recyclerViewAdapters = new ArrayList<>();//适配器列表
    private int screenHeight = 0;//屏幕可用高度，由于横屏转竖屏没有回调，所以记录高度设置
    private int screenWidth = 0;
    private boolean isLand = false;//横竖屏

    public EmotionKeyboardBuilder(Builder builder){
        this.context = builder.context;
        this.type = builder.type;
        EMOTION_CLASSIC_MAP = Builder.EMOTION_CLASSIC_MAP;
        this.matcher = builder.matcher;
        this.editText = builder.editText;
        this.emotionParent = builder.emotionParent;
        this.emotion = builder.emotion;
        this.flower = builder.flower;
        this.rootView = builder.rootView;
        this.onClickSend = builder.onClickSend;
        this.onClickFlower = builder.onClickFlower;
        createEmotionKeyboard();
    }

    private void createEmotionKeyboard(){
        init();
        emotionClickListener();
        buildEmotion();
    }

    /**
     * 初始化
     */
    private void init(){
        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        emotionParent.setVisibility(View.GONE);
        //记录根视图初始宽高
        rootView.post(new Runnable() {
            @Override
            public void run() {
                screenHeight = rootView.getHeight();
                screenWidth = rootView.getWidth();
                Log.e(TAG, "根视图初始高度：" + screenHeight);
                Log.e(TAG, "根视图初始宽度：" + screenWidth);
                vpEmotion.setLayoutParams(new LinearLayout.LayoutParams(screenWidth, screenHeight / 3));
                //由于此时键盘高度为0，先设置一个初始键盘高度给适配器
                for (int i = 0; i < recyclerViewAdapters.size(); i++) {
                    recyclerViewAdapters.get(i).setKeyboardHeight(rootView.getHeight() / 3);
                }
            }
        });
        //根布局视图更新监听
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new GlobalLayoutListener(rootView, new OnKeyboardChangedListener() {
            @Override
            public void onChange(boolean isShow, int keyboardHeight, int screenWidth, int screenHeight) {
                if (EmotionKeyboardBuilder.this.screenHeight < screenHeight) {
                    EmotionKeyboardBuilder.this.screenHeight = screenHeight;
                }
                EmotionKeyboardBuilder.this.onChange(isShow, keyboardHeight, screenWidth, screenHeight);
            }
        }));
    }

    /**
     * 监听
     */
    private void emotionClickListener(){
        emotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.requestFocus();
                if (isKeyboardShow) {
                    //键盘显示的请情况下，隐藏键盘，显示表情布局
                    Log.e(TAG, "键盘显示的请情况下，隐藏键盘，显示表情布局");
                    isEmotionShow = true;
                    EmotionKeyboardBuilder.this.hideKeyboard();
                } else if (isEmotionShow) {
                    //表情布局显示的情况下，隐藏表情布局，显示键盘
                    Log.e(TAG, "表情布局显示的情况下，隐藏表情布局，显示键盘");
                    isEmotionShow = false;
                    EmotionKeyboardBuilder.this.showKeyboard();
                } else {
                    //键盘和表情布局都隐藏的情况下，点击显示表情布局
                    Log.e(TAG, "键盘和表情布局都隐藏的情况下，点击显示表情布局");
                    EmotionKeyboardBuilder.this.showEmotion();
                }
            }
        });
        //发送鲜花接口
        if (flower != null){
            flower.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickFlower != null) {
                        onClickFlower.flower();
                    }
                }
            });
        }
    }

    /**
     * 创建表情布局
     */
    private void buildEmotion(){
        llEmotion = new LinearLayout(context);
        llEmotion.setOrientation(LinearLayout.VERTICAL);
        llEmotion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        llEmotion.setGravity(Gravity.CENTER);
        llEmotion.setBackgroundColor(Color.parseColor("#FFFFFF"));
        vpEmotion = new ViewPager(context);//表情分页
        //关闭越界提示
        vpEmotion.setOverScrollMode(View.OVER_SCROLL_NEVER);
        vpEmotion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        final EmojiIndicatorView emojiIndicatorView = new EmojiIndicatorView(context);//表情分页下方圆点指示器
        emojiIndicatorView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, DisplayUtils.dp2px(context, 30)));
        emojiIndicatorView.setGravity(Gravity.CENTER);
        llEmotion.addView(vpEmotion);
        llEmotion.addView(emojiIndicatorView);
        emotionParent.addView(llEmotion);

        vpEmotion.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int oldPagerPos = 0;
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                emojiIndicatorView.playByStartPointToNext(oldPagerPos, position);
                oldPagerPos = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // 获取屏幕宽度
        int screenWidth = DisplayUtils.getScreenWidthPixels(context);
        // item的间距
        int spacing = DisplayUtils.dp2px(context, 12);
        // 动态计算item的宽度和高度
        int itemWidth = (screenWidth - spacing * 8) / 7;
        List<RecyclerView> recyclerViewList = new ArrayList<>();//网格面板布局
        List<String> emotionNameList = new ArrayList<>();//表情名字列表
        for (String emojiName : EMOTION_CLASSIC_MAP.keySet()){
            emotionNameList.add(emojiName);//将MAP的一一添加进来
            //type==0时20个一组，type==1时19个一组
            if (emotionNameList.size() == 20 - type){
                RecyclerView recyclerView = createEmotionRecyclerView(emotionNameList, spacing, itemWidth);
                recyclerViewList.add(recyclerView);
                emotionNameList = new ArrayList<>();
            }
        }
        //处理最后一组不足20个的情况
        if (emotionNameList.size() > 0){
            RecyclerView recyclerView = createEmotionRecyclerView(emotionNameList, spacing, itemWidth);
            recyclerViewList.add(recyclerView);
        }
        //下方圆点指示器
        emojiIndicatorView.initIndicator(recyclerViewList.size());
        //viewPager适配器
        EmotionPagerAdapter emotionPagerAdapter = new EmotionPagerAdapter(recyclerViewList);
        vpEmotion.setAdapter(emotionPagerAdapter);
    }

    /**
     * 创建recyclerView
     * @param emotionNameList 传入当前recyclerView的表情组合
     * @param padding recyclerView的padding
     * @param itemWidth 子项宽高
     * @return return
     */
    private RecyclerView createEmotionRecyclerView(List<String> emotionNameList, int padding, int itemWidth){
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //设置7列
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 7);
        recyclerView.setPadding(padding, padding, padding, padding);
        vpEmotion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        // 给RecyclerView设置表情图片，传入的初始键盘高度默认500，该值可修改
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(context, emotionNameList, itemWidth, type, rootView.getHeight() / 2);
        //关闭越界提示
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setAdapter(adapter);
        recyclerViewAdapters.add(adapter);//添加适配器到列表，在键盘高度有变化时，通过列表一一传入
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter.setOnClickListener(new RecyclerViewAdapter.OnClickListener() {
            @Override
            public void ItemOnClickListener(String emotionName, int position) {
                //点击表情添加
                StringBuilder sb = new StringBuilder(editText.getText().toString());
                int curPosition = editText.getSelectionStart();
                sb.insert(curPosition, emotionName);
                editText.setText(getEmotionContent(context, editText, sb.toString()));
                editText.setSelection(curPosition + emotionName.length());
            }

            @Override
            public void deleteEmotion() {
                //点击删除回调
                editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            }

            @Override
            public void send() {
                if (onClickSend != null){
                    onClickSend.send();
                }
            }
        });
        return recyclerView;
    }

    /**
     * 键盘监听
     * @param isShow 键盘是否展开
     * @param keyboardHeight 键盘高度
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     */
    public void onChange(boolean isShow, int keyboardHeight, int screenWidth, int screenHeight) {
        Log.e(TAG, "键盘是否展开: " + isShow);
        Log.e(TAG, "键盘高度(px): " + keyboardHeight);
        Log.e(TAG, "屏幕宽度(px): " + screenWidth);
        Log.e(TAG, "屏幕可用高度(px): " + screenHeight);
        //防止竖屏键盘未收起转横屏高度不正确
        if (isLand && screenWidth != 0){
            rootView.getLayoutParams().height = EmotionKeyboardBuilder.this.screenWidth;
        }else {
            //动态改变布局高度跟随键盘的显示和隐藏
            rootView.getLayoutParams().height = screenHeight;
        }
        rootView.requestLayout();
        if (isShow){
            //键盘显示完全，隐藏表情布局
            isKeyboardShow = true;
            hideEmotion();
            //记录键盘高度
            if (keyboardHeight != 0){
                if (this.keyboardHeight == keyboardHeight){
                    return;
                }
                //记录键盘高度
                this.keyboardHeight = keyboardHeight;
                //设置键盘高度到recyclerView的适配器中
                for (int i = 0; i < recyclerViewAdapters.size(); i++){
                    recyclerViewAdapters.get(i).setKeyboardHeight(keyboardHeight);
                }
                vpEmotion.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                //表情布局设置为键盘高度
                emotionParent.getLayoutParams().height = keyboardHeight;
                emotionParent.requestLayout();
            }
        }else {
            //键盘隐藏完全，如果是需要显示表情布局的，则显示
            isKeyboardShow = false;
            if (isEmotionShow){
                showEmotion();
            }
        }
    }

    /**
     * 隐藏表情布局
     */
    public void hideEmotion(){
        isEmotionShow = false;
        emotionParent.setVisibility(View.GONE);
    }

    /**
     * 显示表情布局
     */
    public void showEmotion(){
        isEmotionShow = true;
        emotionParent.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏键盘
     */
    public void hideKeyboard(){
        if (inputMethodManager != null){
            isKeyboardShow = false;
            inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    /**
     * 显示键盘
     */
    public void showKeyboard(){
        if (inputMethodManager != null){
            isKeyboardShow = true;
            inputMethodManager.showSoftInput(editText, 0);
        }
    }

    /**
     * 表情面板是否显示
     */
    public boolean isShowEmotion(){
        return emotionParent.getVisibility() == View.VISIBLE;
    }

    /**
     * 向编辑框添加对应表情
     */
    public SpannableString getEmotionContent(Context context, EditText tv, String source) {
        SpannableString spannableString = new SpannableString(source);
        Resources res = context.getResources();
        String regexEmotion = matcher;//[1|L]
        Pattern patternEmotion = Pattern.compile(regexEmotion);
        Matcher matcherEmotion = patternEmotion.matcher(spannableString);
        while (matcherEmotion.find()) {
            // 获取匹配到的具体字符
            String key = matcherEmotion.group();
            // 匹配字符串的开始位置
            int start = matcherEmotion.start();
            // 利用表情名字获取到对应的图片
            Integer imgRes = EMOTION_CLASSIC_MAP.get(key);
            // 压缩表情图片
            int size = (int) tv.getTextSize() * 13 / 10;
            Bitmap bitmap = BitmapFactory.decodeResource(res, imgRes);
            Bitmap scaleBitmap = null;
            try {
                scaleBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            ImageSpan span = new ImageSpan(context, scaleBitmap);
            spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * 视图更新调用
     */
    public void updateViewMode(){
        if (context != null){
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                //竖屏
                Log.e(TAG, "转到竖屏");
                isLand = false;
                if (screenHeight != 0){
                    rootView.getLayoutParams().height = screenHeight;
                    rootView.requestLayout();
                }
            }else if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                //横屏
                Log.e(TAG, "转到横屏");
                isLand = true;
            }
        }
    }

    /**
     * 分页适配器
     */
    public static class EmotionPagerAdapter extends PagerAdapter {

        private List<RecyclerView> recyclerViewList;

        public EmotionPagerAdapter(List<RecyclerView> recyclerViewList) {
            this.recyclerViewList = recyclerViewList;
        }

        @Override
        public int getCount() {
            return recyclerViewList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(recyclerViewList.get(position));
        }

        @NonNull
        @Override
        public View instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView(recyclerViewList.get(position));
            return recyclerViewList.get(position);
        }
    }

    /**
     * recyclerView适配器
     */
    public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

        private Context context;
        private int type;
        private List<String> emotionNameList;
        private int itemWidth, keyboardHeight;
        private OnClickListener clickListener;

        public void setOnClickListener(OnClickListener clickListener) {
            this.clickListener = clickListener;
        }

        public interface OnClickListener{
            void ItemOnClickListener(String emotionName, int position);
            void deleteEmotion();
            void send();
        }

        public RecyclerViewAdapter(Context context, List<String> emotionNameList, int itemWidth, int type, int keyboardHeight) {
            this.context = context;
            this.type = type;
            this.emotionNameList = emotionNameList;
            this.itemWidth = itemWidth;
            this.keyboardHeight = keyboardHeight;
        }

        /**
         * 传键盘高度便于设置子项高度
         * @param keyboardHeight 当前键盘高度
         */
        public void setKeyboardHeight(int keyboardHeight) {
            this.keyboardHeight = keyboardHeight;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout linearLayout = new LinearLayout(parent.getContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            ViewHolder viewHolder = new ViewHolder(linearLayout);
            viewHolder.viewTop = new View(context);
            viewHolder.viewBottom = new View(context);
            viewHolder.imageView = new ImageView(context);
            viewHolder.textView = new TextView(context);
            linearLayout.addView(viewHolder.viewTop);
            linearLayout.addView(viewHolder.textView);
            linearLayout.addView(viewHolder.imageView);
            linearLayout.addView(viewHolder.viewBottom);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            //子项高度为（当前键盘高度-下方圆点指示器高度-recyclerView的padding高度 * 2）/3
            holder.itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (keyboardHeight - DisplayUtils.dp2px(context, 30) - DisplayUtils.dp2px(context, 12) * 2) / 3));
            Log.e(TAG, "keyboardHeight:" + keyboardHeight);
            holder.viewTop.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            holder.viewBottom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            holder.imageView.setPadding(itemWidth / 8, itemWidth / 8, itemWidth / 8, itemWidth / 8);
            holder.imageView.setLayoutParams(new LinearLayout.LayoutParams(itemWidth, itemWidth));

            if (type == 0){
                if (position == getItemCount() - 1){
                    //如果是最后一个
                    holder.imageView.setImageResource(R.drawable.rc_icon_emoji_delete);
                } else {
                    Glide.with(context).load(EMOTION_CLASSIC_MAP.get(emotionNameList.get(position))).into(holder.imageView);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (clickListener != null) {
                            if (position == RecyclerViewAdapter.this.getItemCount() - 1) {
                                Log.e(TAG, "点击了删除按钮");
                                clickListener.deleteEmotion();
                            } else {
                                Log.e(TAG, "点击了其他表情");
                                clickListener.ItemOnClickListener(emotionNameList.get(position), position);
                            }
                        }
                    }
                });
                holder.textView.setVisibility(View.GONE);
                holder.imageView.setVisibility(View.VISIBLE);
            } else if (type == 1) {
                if (position == getItemCount() - 2){
                    holder.imageView.setImageResource(R.drawable.rc_icon_emoji_delete);
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.textView.setVisibility(View.GONE);
                }else if (position == getItemCount() - 1){
                    holder.textView.setPadding(itemWidth / 8, itemWidth / 8, itemWidth / 8, itemWidth / 8);
                    holder.textView.setLayoutParams(new LinearLayout.LayoutParams(itemWidth, DisplayUtils.dp2px(context, 40)));
                    holder.textView.setBackgroundResource(R.drawable.selector_emotion_send);
                    holder.textView.setText("发送");
                    holder.textView.setTextColor(Color.WHITE);
                    holder.textView.setGravity(Gravity.CENTER);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e(TAG, "点击了发送");
                            if (clickListener != null) {
                                clickListener.send();
                            }
                        }
                    });
                    holder.textView.setVisibility(View.VISIBLE);
                    holder.imageView.setVisibility(View.GONE);
                } else {
                    Glide.with(context).load(EMOTION_CLASSIC_MAP.get(emotionNameList.get(position))).into(holder.imageView);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (clickListener != null) {
                                if (position == RecyclerViewAdapter.this.getItemCount() - 2) {
                                    Log.e(TAG, "点击了删除按钮");
                                    clickListener.deleteEmotion();
                                } else if (position != RecyclerViewAdapter.this.getItemCount() - 1) {
                                    Log.e(TAG, "点击了其他表情");
                                    clickListener.ItemOnClickListener(emotionNameList.get(position), position);
                                }
                            }
                        }
                    });
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.textView.setVisibility(View.GONE);

                }
            }
        }

        @Override
        public int getItemCount() {
            Log.e(TAG, "getItemCount" + (emotionNameList.size() + 1 + type));
            return emotionNameList.size() + 1 + type;
        }

        static class ViewHolder extends RecyclerView.ViewHolder{
            View viewTop, viewBottom;
            ImageView imageView;
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    /**
     * 构造者模式
     */
    public static final class Builder{
        private Activity context;
        private int type = 0;//表情布局类型，0为不带发送按钮的，1为带发送按钮的
        private static ArrayMap<String, Integer> EMOTION_CLASSIC_MAP;
        private String matcher;//传入正则表达式
        private EditText editText;//编辑框
        private View emotion;//点击弹出表情框的按钮
        private View flower;//发鲜花
        private ViewGroup emotionParent;//装表情的总布局
        private ViewGroup rootView;//页面的根布局
        private OnClickSend onClickSend;//发送接口
        private OnClickFlower onClickFlower;//发送鲜花接口

        public Builder(Activity context){
            this.context = context;
        }

        /**
         * 绑定表情布局类型
         * @param type 类型
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindType(int type){
            this.type = type;
            if (type != 0 && type != 1){
                this.type = 0;
            }
            return this;
        }

        /**
         * 绑定表情map
         * @param EMOTION_CLASSIC_MAP map
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindEmotionMap(ArrayMap<String, Integer> EMOTION_CLASSIC_MAP){
            Builder.EMOTION_CLASSIC_MAP = EMOTION_CLASSIC_MAP;
            return this;
        }

        /**
         * map对应的正则表达式
         * @param matcher 正则表达式
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindMatcher(String matcher){
            this.matcher = matcher;
            return this;
        }

        /**
         * 绑定编辑框
         * @param editText 编辑框
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindEditText(EditText editText){
            this.editText = editText;
            return this;
        }

        /**
         * 绑定表情按钮
         * @param emotion 表情按钮
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindEmotionView(View emotion){
            this.emotion = emotion;
            return this;
        }

        /**
         * 绑定发送鲜花按钮
         * @param flower 鲜花按钮
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindFlowerView(View flower){
            this.flower = flower;
            return this;
        }

        /**
         * 绑定表情框的占位view
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindBottomView(ViewGroup emotionParent){
            this.emotionParent = emotionParent;
            return this;
        }

        /**
         * 绑定根布局
         * @return return
         */
        public EmotionKeyboardBuilder.Builder bindRootView(ViewGroup rootView){
            this.rootView = rootView;
            return this;
        }

        /**
         * 发送接口
         * @param onClickSendListener 发送接口
         * @return return
         */
        public EmotionKeyboardBuilder.Builder setOnClickSendListener(OnClickSend onClickSendListener){
            if (type != 1){
                return this;
            }
            this.onClickSend = onClickSendListener;
            return this;
        }

        public EmotionKeyboardBuilder.Builder setOnClickFlowerListener(OnClickFlower onClickFlowerListener){
            this.onClickFlower = onClickFlowerListener;
            return this;
        }

        public EmotionKeyboardBuilder build(){
            return new EmotionKeyboardBuilder(this);
        }
    }

    public interface OnClickSend{
        void send();
    }

    public interface OnClickFlower{
        void flower();
    }
}
