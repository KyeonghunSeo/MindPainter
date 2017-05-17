package com.hellowo.mindpainter;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.hellowo.mindpainter.utils.AnimationUtil;
import com.hellowo.mindpainter.utils.ByteUtil;

public class EvaluateDialog extends Dialog {
    MainActivity activity;
    CardView rootLy;
    TextView roundText;
    TextView subText;
    LottieAnimationView[] animationView;
    Button[] evaluateBtn;

    boolean isMyTurn;
    int playerNum;
    int evaluatePlayerNum;
    public static String[] animationName = new String[]{"Wink.json", "Shock.json", "Tongue.json"};

    public EvaluateDialog(MainActivity activity, boolean isMyTurn, int playerNum) {
        super(activity);
        this.activity = activity;
        this.isMyTurn = isMyTurn;
        this.playerNum = playerNum;
        this.evaluatePlayerNum = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_evaluate);
        setLayout();

        roundText.setText(R.string.evaluate);

        if(isMyTurn) {

            subText.setText(R.string.waiting_evaluate);

            animationView[1].setAnimation("Loading 1.json");
            animationView[1].loop(true);
            animationView[1].playAnimation();

            animationView[0].setVisibility(View.GONE);
            animationView[2].setVisibility(View.GONE);
            evaluateBtn[0].setVisibility(View.GONE);
            evaluateBtn[1].setVisibility(View.GONE);
            evaluateBtn[2].setVisibility(View.GONE);

        }else {

            subText.setText(R.string.evaluate_this_round);

            evaluateBtn[0].setText(R.string.good_sense);
            evaluateBtn[1].setText(R.string.soso);
            evaluateBtn[2].setText(R.string.bad);

            for (int i = 0; i < 3; i++) {
                final int finalI = i;

                animationView[i].setAnimation(animationName[i]);
                animationView[i].loop(true);
                animationView[i].playAnimation();
                evaluateBtn[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        evaluate(finalI);
                    }
                });
            }
        }
    }

    private void evaluate(int evaluateNum) {
        GameMessage msg = MainActivity.makeMessage(
                203,
                null,
                evaluateNum,
                0,
                0,
                System.currentTimeMillis()
        );
        activity.broadcastScore(true, ByteUtil.toByteArray(msg));

        animationView[1].setAnimation("Loading 1.json");
        animationView[1].loop(true);
        animationView[1].playAnimation();

        animationView[0].cancelAnimation();
        animationView[2].cancelAnimation();

        animationView[0].setVisibility(View.GONE);
        animationView[2].setVisibility(View.GONE);
        evaluateBtn[0].setVisibility(View.GONE);
        evaluateBtn[1].setVisibility(View.GONE);
        evaluateBtn[2].setVisibility(View.GONE);

        subText.setText(R.string.finish_evaluate);
        AnimationUtil.startScaleShowAnimation(subText);
    }

    public void receiveEvaluate(int evaluateNum) {
        evaluatePlayerNum++;
        if(playerNum == evaluatePlayerNum) {
            GameMessage msg = MainActivity.makeMessage(
                    204,
                    null,
                    evaluateNum,
                    0,
                    0,
                    System.currentTimeMillis()
            );
            activity.broadcastScore(true, ByteUtil.toByteArray(msg));
            activity.startNextRound();
            dismiss();
        }
    }

    private void setLayout() {
        rootLy = (CardView)findViewById(R.id.rootLy);
        roundText = (TextView)findViewById(R.id.roundText);
        subText = (TextView)findViewById(R.id.subText);

        animationView = new LottieAnimationView[3];
        animationView[0] = (LottieAnimationView)findViewById(R.id.animView1);
        animationView[1] = (LottieAnimationView)findViewById(R.id.animView2);
        animationView[2] = (LottieAnimationView)findViewById(R.id.animView3);

        evaluateBtn = new Button[3];
        evaluateBtn[0] = (Button)findViewById(R.id.evaluateBtn1);
        evaluateBtn[1] = (Button)findViewById(R.id.evaluateBtn2);
        evaluateBtn[2] = (Button)findViewById(R.id.evaluateBtn3);

        rootLy.setVisibility(View.INVISIBLE);
    }

    @Override
    public void show() {
        super.show();
        rootLy.postDelayed(new Runnable() {
            @Override
            public void run() {
                rootLy.setVisibility(View.VISIBLE);
                AnimationUtil.startScaleShowAnimation(rootLy);
            }
        }, 250);
    }

    public void finishEvaluate() {
        activity.startNextRound();
        dismiss();
    }
}
