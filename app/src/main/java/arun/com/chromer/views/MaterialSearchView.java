package arun.com.chromer.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import arun.com.chromer.R;

public class MaterialSearchView extends FrameLayout {
    int normalColor = ContextCompat.getColor(getContext(), R.color.accent_icon_nofocus);
    int focusedColor = ContextCompat.getColor(getContext(), R.color.accent_icon_focused);
    boolean animated = false;
    private ImageView leftIcon;
    private ImageView rightIcon;
    private TextView label;
    private EditText editText;
    private CardView card;
    private int labelTopMargin;

    public MaterialSearchView(Context context) {
        super(context);
        init(null, 0);
    }

    public MaterialSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MaterialSearchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.MaterialSearchView, defStyle, 0);

    }

    private void toggle() {
        if (animated) loseFocus();
        else gainFocus();
    }

    private void loseFocus() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                getColorChangeAnimatorOnImageView(leftIcon, focusedColor, normalColor, 400),
                getColorChangeAnimatorOnImageView(rightIcon, focusedColor, normalColor, 400),
                ObjectAnimator.ofFloat(label, "scaleX", 1),
                ObjectAnimator.ofFloat(label, "scaleY", 1),
                ObjectAnimator.ofFloat(label, "translationY", 0),

                ObjectAnimator.ofFloat(editText, "alpha", 0).setDuration(300)
        );

        animatorSet.start();
        clearFocusFromEditText();

        // Hide the keyboard
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

        animated = false;
    }

    private void clearFocusFromEditText() {
        rightIcon.requestFocus();
    }

    private void gainFocus() {

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                getColorChangeAnimatorOnImageView(leftIcon, normalColor, focusedColor, 300),
                getColorChangeAnimatorOnImageView(rightIcon, normalColor, focusedColor, 300),
                ObjectAnimator.ofFloat(label, "scaleX", 0.6f),
                ObjectAnimator.ofFloat(label, "scaleY", 0.6f),
                ObjectAnimator.ofFloat(label, "translationY", -labelTopMargin),

                ObjectAnimator.ofFloat(editText, "alpha", 1).setDuration(300)
        );


        animated = true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Inflate and add the xml layout we designed
        addView(LayoutInflater.from(getContext()).inflate(R.layout.material_search_view, this, false));

        editText = (EditText) findViewById(R.id.msv_edittext);
        editText.clearFocus();
        editText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                performClick();
            }
        });
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    gainFocus();
                else
                    loseFocus();
            }
        });

        leftIcon = (ImageView) findViewById(R.id.msv_left_icon);
        leftIcon.setImageDrawable(new IconicsDrawable(getContext())
                .icon(GoogleMaterial.Icon.gmd_search)
                .color(normalColor)
                .sizeDp(24));

        rightIcon = (ImageView) findViewById(R.id.msv_right_icon);
        rightIcon.setImageDrawable(new IconicsDrawable(getContext())
                .icon(GoogleMaterial.Icon.gmd_keyboard_voice)
                .color(normalColor)
                .sizeDp(24));

        label = (TextView) findViewById(R.id.msv_label);
        label.setPivotX(0);
        label.setPivotY(0);

        labelTopMargin = FrameLayout.LayoutParams.class.cast(label.getLayoutParams()).topMargin;

        card = (CardView) findViewById(R.id.msv_card);

        //// FIXME: 29/01/2016
        clearFocusFromEditText();

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

    }

    private Animator getColorChangeAnimatorOnImageView(final ImageView viewToAnimate, int fromColor, int toColor, int duration) {
        final float[] from = new float[3],
                to = new float[3];

        Color.colorToHSV(fromColor, from);
        Color.colorToHSV(toColor, to);

        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(duration);

        final float[] hsv = new float[3];
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                hsv[0] = from[0] + (to[0] - from[0]) * animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1]) * animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2]) * animation.getAnimatedFraction();

                IconicsDrawable drawable = (IconicsDrawable) viewToAnimate.getDrawable();
                viewToAnimate.setImageDrawable(drawable.color(Color.HSVToColor(hsv)));
            }
        });
        return anim;
    }


    public int adjustAlpha(int color, float factor) {
        return Color.argb(Math.round(Color.alpha(color) * factor),
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
