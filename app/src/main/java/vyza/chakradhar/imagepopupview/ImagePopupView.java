package vyza.chakradhar.imagepopupview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * Created by macuser on 07/11/17.
 */

public class ImagePopupView extends AppCompatImageView implements View.OnClickListener {

    private Animator mCurrentAnimator;
    private static final int mShortAnimationDuration = 300;
    private PopupViewImpl popupViewImpl = null;
    private int layoutId = R.layout.image_popup_default;

    public ImagePopupView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public interface PopupViewImpl {
        void setPopupView();
    }

    /*
    * This method can be used to override the popup view with custom view
    * only criteria is that ImageView inside custom view should have the id imageView
    * because animation happens on the imageView
    * */
    private void setPopupView(int layoutId, PopupViewImpl popupViewImpl) {
        if (layoutId <= 0) {
            throw new IllegalArgumentException("Valid layout id not provided");
        }
        if (popupViewImpl == null) {
            throw new IllegalArgumentException("PopupViewImpl cannot be null");
        }
        this.layoutId = layoutId;
        this.popupViewImpl = popupViewImpl;
    }

    private void openPopup() {
        View view = createPopupView();
        final android.support.v7.app.AlertDialog dialog = new android.support.v7.app.AlertDialog.Builder(getContext()).setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_UP &&
                        !event.isCanceled()) {
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        }).create();
        final Rect startBounds = new Rect();
        final ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        imageView.getGlobalVisibleRect(startBounds);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setView(view);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }
                imageView.setPivotX(startBounds.right);
                imageView.setPivotY(startBounds.top - (startBounds.bottom - startBounds.top) / 3);
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(imageView, View.SCALE_X,
                                0.35f, 1f))
                        .with(ObjectAnimator.ofFloat(imageView,
                                View.SCALE_Y, 0.35f, 1f))
                        .with(ObjectAnimator.ofFloat(imageView, View.ALPHA, 0.25f, 1f));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.start();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                reverseAnimation(imageView, startBounds, 0.2f);
            }
        });
        dialog.show();
    }

    private View createPopupView() {
        View view = LayoutInflater.from(getContext()).inflate(layoutId, null);
        if (popupViewImpl == null) {
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
            if (getTag() != null && getTag() instanceof Bitmap) {
                imageView.setImageBitmap((Bitmap) getTag());
            } else
                imageView.setImageDrawable(getDrawable());
        } else {
            // user can set views based on the custom layout used
            popupViewImpl.setPopupView();
        }
        return view;
    }

    private void reverseAnimation(final ImageView expandedImageView, Rect startBounds, float startScaleFinal) {
        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator
                .ofFloat(expandedImageView, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_X, 0.65f, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_Y, 0.65f, startScaleFinal))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.ALPHA, 0.35f, 0));
        set.setDuration(mShortAnimationDuration * 2);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                expandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                expandedImageView.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        expandedImageView.setVisibility(View.VISIBLE);
        set.start();
        mCurrentAnimator = set;
    }

    @Override
    public void onClick(View v) {
        openPopup();
    }
}
