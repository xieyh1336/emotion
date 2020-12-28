package com.example.emotionkeyboardbuilder.util.gif;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.ArrayMap;
import android.widget.TextView;

import com.example.emotionkeyboardbuilder.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 该工具可将gif添加入textView
 * 使用了以下工具类
 * {@link AnimatedGifDrawable}
 * {@link AnimatedImageSpan}
 * {@link GifDecoder}
 */
public class GIfInTextView {

    public static ArrayMap<String, Integer> EMOTION_CLASSIC_MAP = new ArrayMap<>();
    public static ArrayMap<String, String> EMOTION_CLASSIC_MAP2 = new ArrayMap<>();

    static {
        EMOTION_CLASSIC_MAP.put("[0|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[1|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[2|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[3|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[4|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[5|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[6|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[7|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[8|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[9|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[10|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[11|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[12|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[13|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[14|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[15|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[16|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[17|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[18|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[19|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[20|L]", R.drawable.gif2);
        EMOTION_CLASSIC_MAP.put("[21|L]", R.drawable.gif0);
        EMOTION_CLASSIC_MAP.put("[22|L]", R.drawable.gif1);
        EMOTION_CLASSIC_MAP.put("[23|L]", R.drawable.gif2);

        EMOTION_CLASSIC_MAP2.put("[0|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[1|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[2|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[3|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[4|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[5|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[6|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[7|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[8|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[9|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[10|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[11|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[12|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[13|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[14|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[15|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[16|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[17|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[18|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[19|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[20|L]", "gif2.gif");
        EMOTION_CLASSIC_MAP2.put("[21|L]", "gif0.gif");
        EMOTION_CLASSIC_MAP2.put("[22|L]", "gif1.gif");
        EMOTION_CLASSIC_MAP2.put("[23|L]", "gif2.gif");
    }


    public static SpannableStringBuilder textToGif( final TextView gifTextView, String regexEmotion, SpannableStringBuilder stringBuilder, Context context){
        //以下为将表情转换为gif的代码
        Pattern patternEmotion = Pattern.compile(regexEmotion);
        Matcher matcherEmotion = patternEmotion.matcher(stringBuilder);
        while (matcherEmotion.find()) {
            String tempText = matcherEmotion.group();
            String gif = EMOTION_CLASSIC_MAP2.get(matcherEmotion.group());
            try {
                //如果open这里不抛异常说明存在gif，则显示对应的gif
                //否则说明gif找不到，则显示png
                InputStream is = context.getAssets().open(gif);
                stringBuilder.setSpan(new AnimatedImageSpan(new AnimatedGifDrawable(is,
                                new AnimatedGifDrawable.UpdateListener() {
                                    @Override
                                    public void update() {
                                        gifTextView.postInvalidate();
                                    }
                                })), matcherEmotion.start(), matcherEmotion.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                is.close();
            } catch (Exception e) {
                String png = tempText.substring("[".length(),
                        tempText.length() - "]".length());
                try {
                    stringBuilder.setSpan(
                            new ImageSpan(context, BitmapFactory
                                    .decodeStream(context.getAssets()
                                            .open(png))), matcherEmotion.start(), matcherEmotion.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        return stringBuilder;
    }
}
