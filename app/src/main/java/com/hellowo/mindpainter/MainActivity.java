/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hellowo.mindpainter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.hellowo.mindpainter.utils.AnimationUtil;
import com.hellowo.mindpainter.utils.ByteUtil;
import com.hellowo.mindpainter.utils.DialogUtil;
import com.hellowo.mindpainter.utils.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.hellowo.mindpainter.InkView.TOOL_SIGN_PEN;

public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    final static String TAG = "Mide Painter";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Are we playing in multiplayer mode?
    boolean mMultiplayer = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;
    HashMap<String, Player> playerMap = new HashMap<>();

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

        initInkView();
        initPlayerLayout();
        initChatListView();
        initInputView();
        initDrawToolView();
        initQuestionView();
        initFab();
        initProgressView();
        initOhterViews();
    }

    InkView inkView;
    int inkViewWidth;
    int inkViewHeight;
    int deviceHeight;

    private void initInkView() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        inkView = (InkView) findViewById(R.id.ink);
        inkViewWidth = dm.widthPixels;
        deviceHeight = dm.heightPixels - getStatusBarHeight();
        inkViewHeight = (int) (inkViewWidth * 1.5f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                inkViewWidth,
                inkViewHeight
        );
        lp.setMargins(0, 0, 0, ViewUtil.dpToPx(this, 50));
        lp.gravity = Gravity.BOTTOM;
        inkView.setLayoutParams(lp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            inkView.setElevation(ViewUtil.dpToPx(this, 1));
        }

        inkView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            inkView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            inkView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        inkView.init();
                    }
                });

        inkView.setBackgroundColor(Color.WHITE);
        inkView.addListener(new InkView.InkListener() {
            @Override
            public void onInkClear() {}

            @Override
            public void onDraw(int action, int pNum, float x, float y, long time) {
                GameMessage msg;

                if(action == MotionEvent.ACTION_DOWN) {
                    msg = makeMessage(0, null, pNum, x, y, time);
                }else if(action == MotionEvent.ACTION_MOVE) {
                    msg = makeMessage(1, null, pNum, x, y, time);
                }else {
                    msg = makeMessage(2, null, pNum, x, y, time);
                }

                broadcastScore(true, ByteUtil.toByteArray(msg));
            }

            @Override
            public void onSetColor(int pNum, int color) {
                GameMessage msg = makeMessage(3, String.valueOf(color), pNum, 0, 0, System.currentTimeMillis());
                broadcastScore(true, ByteUtil.toByteArray(msg));
            }

            @Override
            public void onSetTool(int pNum, int tool) {
                GameMessage msg = makeMessage(4, String.valueOf(tool), pNum, 0, 0, System.currentTimeMillis());
                broadcastScore(true, ByteUtil.toByteArray(msg));
            }
        });
    }

    FrameLayout[] pLayouts = new FrameLayout[MAX_PLAYER_COUNT];
    TextView[] pNameViews = new TextView[MAX_PLAYER_COUNT];
    TextView[] pScoreViews = new TextView[MAX_PLAYER_COUNT];
    TextView[] pDrawScoreViews = new TextView[MAX_PLAYER_COUNT];
    CircleImageView[] pImgViews = new CircleImageView[MAX_PLAYER_COUNT];
    TextView[] pPenaltyViews = new TextView[MAX_PLAYER_COUNT];
    LottieAnimationView mainAnimationView;

    private void initPlayerLayout() {
        pLayouts[0] = (FrameLayout) findViewById(R.id.p1Ly);
        pNameViews[0] = (TextView) findViewById(R.id.p1Name);
        pImgViews[0] = (CircleImageView) findViewById(R.id.p1Img);
        pScoreViews[0] = (TextView) findViewById(R.id.p1Score);
        pDrawScoreViews[0] = (TextView) findViewById(R.id.p1DrawScore);
        pPenaltyViews[0] = (TextView) findViewById(R.id.p1Penalty);

        pLayouts[1] = (FrameLayout) findViewById(R.id.p2Ly);
        pNameViews[1] = (TextView) findViewById(R.id.p2Name);
        pImgViews[1] = (CircleImageView) findViewById(R.id.p2Img);
        pScoreViews[1] = (TextView) findViewById(R.id.p2Score);
        pDrawScoreViews[1] = (TextView) findViewById(R.id.p2DrawScore);
        pPenaltyViews[1] = (TextView) findViewById(R.id.p2Penalty);

        pLayouts[2] = (FrameLayout) findViewById(R.id.p3Ly);
        pNameViews[2] = (TextView) findViewById(R.id.p3Name);
        pImgViews[2] = (CircleImageView) findViewById(R.id.p3Img);
        pScoreViews[2] = (TextView) findViewById(R.id.p3Score);
        pDrawScoreViews[2] = (TextView) findViewById(R.id.p3DrawScore);
        pPenaltyViews[2] = (TextView) findViewById(R.id.p3Penalty);

        pLayouts[3] = (FrameLayout) findViewById(R.id.p4Ly);
        pNameViews[3] = (TextView) findViewById(R.id.p4Name);
        pImgViews[3] = (CircleImageView) findViewById(R.id.p4Img);
        pScoreViews[3] = (TextView) findViewById(R.id.p4Score);
        pDrawScoreViews[3] = (TextView) findViewById(R.id.p4DrawScore);
        pPenaltyViews[3] = (TextView) findViewById(R.id.p4Penalty);

        mainAnimationView = (LottieAnimationView) findViewById(R.id.mainAnimationView);
    }

    RecyclerView chatListView;
    ChatListAdapter chatAdapter;

    private void initChatListView() {
        chatListView = (RecyclerView) findViewById(R.id.chatListView);
        chatListView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatListAdapter(this);
        chatListView.setAdapter(chatAdapter);
    }

    public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

        private List<Chat> chatList;
        long aniamtionOffset;

        public ChatListAdapter(Context context){
            chatList = new ArrayList<>();
            aniamtionOffset = ViewUtil.dpToPx(context, 20);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            CircleImageView imageView;
            TextView nameText;
            TextView chatText;
            View root;

            public ViewHolder(View v) {
                super(v);
                root = v;
                imageView = (CircleImageView)v.findViewById(R.id.imageView);
                nameText = (TextView)v.findViewById(R.id.nameText);
                chatText = (TextView)v.findViewById(R.id.chatText);
            }
        }

        @Override
        public ChatListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatListAdapter.ViewHolder holder, final int position) {
            Chat chat = getItem(position);
            holder.nameText.setText(chat.player.name + " : ");
            holder.chatText.setText(chat.message);

            if(chat.type == 100) {
                holder.chatText.setTextColor(Color.BLACK);
            }else {
                holder.chatText.setTextColor(Color.RED);
            }

            //AnimationUtil.startFromBottomSlideAppearAnimation(holder.root, aniamtionOffset);
        }

        public Chat getItem(int position) {
            return chatList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        public void addChat(Chat chat) {
            chatList.add(chat);
            notifyItemInserted(chatList.size() - 1);
            chatListView.scrollToPosition(chatList.size() - 1);
        }

        public void clear() {
            chatList.clear();
            notifyDataSetChanged();
        }
    }

    public class Chat {
        String message;
        Player player;
        int type;
    }

    FrameLayout inputLy;
    ImageButton answerBtn;
    View answerView;
    EditText input;
    Button sendBtn;

    private void initInputView() {
        inputLy = (FrameLayout) findViewById(R.id.inputLy);
        input = (EditText) findViewById(R.id.input);
        answerBtn = (ImageButton) findViewById(R.id.answerBtn);
        answerView = findViewById(R.id.answerView);
        sendBtn = (Button) findViewById(R.id.sendBtn);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        sendInputMessage();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendInputMessage();
            }
        });

        answerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(chatMode == CHAT_NORMAL) {
                    setChatMode(CHAT_ANSWER);
                    AnimationUtil.startScaleHideAnimation(answerBtn);
                    getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    ((InputMethodManager) getSystemService
                            (Context.INPUT_METHOD_SERVICE)).showSoftInput(input, 0);
                }
            }
        });
    }

    DrawingToolView drawingToolView;

    private void initDrawToolView() {
        drawingToolView = (DrawingToolView) findViewById(R.id.drawToolLy);
        drawingToolView.init(this, inkView);
        drawingToolView.setTranslationY(deviceHeight);
    }

    TextView questionLengthText;

    private void initQuestionView() {
        questionLengthText = (TextView) findViewById(R.id.questionLengthText);
        questionLengthText.setText("0");
    }

    private void initFab() {
    }

    FrameLayout interfaceLy;
    RoundCornerProgressBar tickProgress;
    TextView tickProgressText;

    private void initProgressView() {
        interfaceLy = (FrameLayout) findViewById(R.id.interfaceLy);
        tickProgress = (RoundCornerProgressBar) findViewById(R.id.tickProgress);
        tickProgress.setBackgroundColor(Color.LTGRAY);
        tickProgressText = (TextView) findViewById(R.id.tickProgressText);
        interfaceLy.setTranslationY(-ViewUtil.dpToPx(this, 25));
    }

    TextView popupText;

    private void initOhterViews() {
        popupText = (TextView) findViewById(R.id.popupText);
        popupText.setScaleX(0);
        popupText.setScaleY(0);
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_single_player:
            case R.id.button_single_player_2:
                resetGameVars();
                startGame(false);
                break;
            case R.id.button_sign_in:
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_SELECT_PLAYERS);
                break;
            case R.id.button_see_invitations:
                intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                startQuickGame();
                break;
        }
    }

    void startQuickGame() {
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                handleSelectPlayersResult(responseCode, intent);
                break;
            case RC_INVITATION_INBOX:
                handleInvitationInboxResult(responseCode, intent);
                break;
            case RC_WAITING_ROOM:
                if (responseCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    startGame(true);
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    leaveRoom();
                }
                break;
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + responseCode + ", intent=" + intent);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    BaseGameUtils.showActivityResultError(this,requestCode,responseCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();
        // stop trying to keep the screen on
        stopKeepingScreenOn();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToMainScreen();
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
        super.onStop();
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        if (mGoogleApiClient == null) {
            switchToScreen(R.id.screen_sign_in);
        } else if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG,"Connecting client.");
            switchToScreen(R.id.screen_wait);
            mGoogleApiClient.connect();
        } else {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        }
        super.onStart();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        leftTime = -1;
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
    }

    @Override
    public void onInvitationRemoved(String invitationId) {

        if (mIncomingInvitationId.equals(invitationId)&&mIncomingInvitationId!=null) {
            mIncomingInvitationId = null;
            switchToScreen(mCurScreen); // This will hide the invitation popup
        }

    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG,"onConnected: connection hint has a room invite!");
                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }
        switchToMainScreen();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }

        switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        //get participants and my ID:
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if(mRoomId==null) {
            mRoomId = room.getRoomId();
        }
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
        switchToMainScreen();
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        mRoomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {}

    @Override
    public void onP2PConnected(String participant) {}

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();

            playerMap.clear();

            for (Participant p : mParticipants) {
                Player player = new Player();
                player.id = p.getParticipantId();
                player.name = p.getDisplayName();
                playerMap.put(p.getParticipantId(), player);
            }

            setPlayerViews();
        }
    }

    /**********************************************************************
     * 게임 로직 섹션 Methods that implement the game's rules.
     **********************************************************************/

    // Current state of the game:
    final static int MAX_PLAYER_COUNT = 4;
    final static int GAME_STATUS_READYING = 0;
    final static int GAME_STATUS_DRAWING = 1;
    final static int GAME_STATUS_CORRECT_ANSWER = 2;
    final static int GAME_STATUS_TIME_LIMIT = 3;
    final static int GAME_TICK_FRAME = 35;
    final static int PLAYER_PANELTY_SECONDS = 10;
    final static int CHAT_NORMAL = 0;
    final static int CHAT_ANSWER = 1;

    long gameDuration = 1000 * 60; // 그리기 시간
    long leftTime = -1; // 남은 시간
    int[] playerAnswerPenalty = new int[MAX_PLAYER_COUNT];
    int pNum = 0; // 현재 그려지는 포인트 넘버
    int gameStatus = GAME_STATUS_READYING;
    int chatMode = CHAT_NORMAL;
    int round = 1;
    int currentTurnPlayerIndex;
    String currentTurnPlayerId;
    HashMap<Integer, GameMessage> savedPNumMap = new HashMap<>();

    QuestionDialog questionDialog;
    EvaluateDialog evaluateDialog;

    void resetGameVars() {
        pNum = 0;
        savedPNumMap.clear();
        chatMode = CHAT_NORMAL;
        for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
            playerAnswerPenalty[i] = 0;
        }
    }

    private void resetView() {
        inkView.clear();
        tickProgress.setProgress(100);
        tickProgressText.setText("");

        for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
            pPenaltyViews[i].setVisibility(View.GONE);
        }

        input.setTextColor(getResources().getColor(R.color.primary_text));
        input.setHintTextColor(getResources().getColor(R.color.disable_text));
        input.setHint(R.string.input_hint);
        answerView.setScaleX(0f);
        answerView.setScaleY(0f);
        answerBtn.setScaleX(1f);
        answerBtn.setScaleY(1f);

        questionLengthText.setText("");
    }

    void startGame(boolean multiplayer) {
        mMultiplayer = multiplayer;

        switchToScreen(R.id.screen_game);
        clearPlayerView();
        resetView();

        if(multiplayer) {

            setPlayerViews();
            setNextTurnPlayer();
            setBottomViews();
            sendMyInformation();
            startQuestionSelect();

        }else {

            setBottomViews();
            inkView.startDrawing();

        }
    }

    private void startQuestionSelect() {
        gameStatus = GAME_STATUS_READYING;
        inkView.stopDrawing();

        hideKeyPad();

        questionDialog = new QuestionDialog(this, round, isMyTurn());
        DialogUtil.showDialog(questionDialog, false, true, true, false);
    }

    public void setQuestion() {
        if(isMyTurn()) {
            questionLengthText.setText(String.valueOf(Question.getCurrentQuestion()));
        }else{
            questionLengthText.setText("x " + String.valueOf(Question.getCurrentQuestion().length()));
        }
        AnimationUtil.startScaleShowAnimation(questionLengthText);
    }

    public void startDrawing() {
        gameStatus = GAME_STATUS_DRAWING;
        showPopupText(
                getString(R.string.start),
                getResources().getColor(R.color.colorPrimary),
                1500
        );
        startTickProgress();

        if(isMyTurn()) {
            inkView.startDrawing();
        }
    }

    private void startTickProgress() {
        leftTime = gameDuration;
        AnimationUtil.startFromTopSlideAppearAnimation(interfaceLy, ViewUtil.dpToPx(this, 25));

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (gameStatus == GAME_STATUS_TIME_LIMIT || gameStatus == GAME_STATUS_CORRECT_ANSWER){

                    if(gameStatus == GAME_STATUS_TIME_LIMIT && isMyTurn()){
                        GameMessage msg = MainActivity.makeMessage(
                                205/*timelimit*/, null, 0, 0, 0, System.currentTimeMillis());
                        broadcastScore(true, ByteUtil.toByteArray(msg));
                        timeLimited();
                    }

                    return;
                }
                gameTick();
                h.postDelayed(this, GAME_TICK_FRAME);
            }
        }, GAME_TICK_FRAME);
    }

    void gameTick() {
        if (leftTime > 0) {
            leftTime -= GAME_TICK_FRAME;
        } else if(gameStatus == GAME_STATUS_DRAWING) {
            gameStatus = GAME_STATUS_TIME_LIMIT;
        }

        tickProgress.setProgress(((float) leftTime / gameDuration) * 100);
        tickProgressText.setText(String.valueOf(leftTime / 1000));
    }

    private void setChatMode(int chatMode) {
        this.chatMode = chatMode;

        if(chatMode == CHAT_NORMAL) {
            input.setHint(R.string.input_hint);
            input.setTextColor(getResources().getColor(R.color.primary_text));
            input.setHintTextColor(getResources().getColor(R.color.disable_text));
            AnimationUtil.startScaleHideAnimation(answerView);
        }else {
            input.setHint(R.string.input_answer_hint);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(Color.WHITE);
            AnimationUtil.startScaleShowAnimation(answerView);
        }
    }

    public void incorrectAnswer(Player player) {
        if(player.id.equals(mMyId)) {
            showPopupText(getString(R.string.incorrectAnswer),
                    getResources().getColor(R.color.redIndentity),
                    2000
            );

            answerBtn.setEnabled(false);
            answerBtn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    answerBtn.setEnabled(true);
                    AnimationUtil.startScaleShowAnimation(answerBtn);
                }
            }, 1000 * PLAYER_PANELTY_SECONDS);
        }

        final int playerIndex = getPlayerIndex(player);
        playerAnswerPenalty[playerIndex] = PLAYER_PANELTY_SECONDS;
        pPenaltyViews[playerIndex].setVisibility(View.VISIBLE);
        pPenaltyViews[playerIndex].setText(String.valueOf(PLAYER_PANELTY_SECONDS));

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (playerAnswerPenalty[playerIndex] <= 0){
                    pPenaltyViews[playerIndex].setVisibility(View.GONE);
                    return;
                }
                playerAnswerPenalty[playerIndex]--;
                pPenaltyViews[playerIndex].setText(String.valueOf(playerAnswerPenalty[playerIndex]));
                h.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public void correctAnswer(Player player) {
        gameStatus = GAME_STATUS_CORRECT_ANSWER;

        AnimationUtil.startToTopDisappearAnimation(interfaceLy, ViewUtil.dpToPx(this, 25));

        player.correctAnswerCount++;
        pScoreViews[getPlayerIndex(player)].setText(String.valueOf(player.correctAnswerCount));

        Player currentTurnPlayer = playerMap.get(currentTurnPlayerId);
        currentTurnPlayer.successDrawingCount++;
        pDrawScoreViews[getPlayerIndex(currentTurnPlayer)]
                .setText(String.valueOf(currentTurnPlayer.successDrawingCount));

        if(player.id.equals(mMyId)) {
            showPopupText(
                    getString(R.string.correctAnswer),
                    getResources().getColor(R.color.colorPrimary),
                    2000
            );
        }else {
            showPopupText(
                    String.format(getString(R.string.correctAnswerPlayer), player.name),
                    getResources().getColor(R.color.colorPrimary),
                    2000
            );
        }

        if(player.id.equals(mMyId) || currentTurnPlayer.id.equals(mMyId)) {
            mainAnimationView.setAnimation("Favorite Star.json");
        }else {
            mainAnimationView.setAnimation("Tongue.json");
        }

        mainAnimationView.setVisibility(View.VISIBLE);
        AnimationUtil.startScaleShowAnimation(mainAnimationView);
        mainAnimationView.playAnimation();

        interfaceLy.postDelayed(new Runnable() {
            @Override
            public void run() {
                AnimationUtil.startScaleHideAnimation(mainAnimationView);
                mainAnimationView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mainAnimationView.cancelAnimation();
                        mainAnimationView.setVisibility(View.GONE);
                    }
                }, 250);
                startEvaluate();
            }
        }, 2000);
    }

    public void startEvaluate() {
        AnimationUtil.startToTopDisappearAnimation(interfaceLy, ViewUtil.dpToPx(this, 25));
        evaluateDialog = new EvaluateDialog(this, isMyTurn(), mParticipants.size() - 1);
        DialogUtil.showDialog(evaluateDialog, false, true, true, false);
    }

    private void timeLimited() {
        AnimationUtil.startToTopDisappearAnimation(interfaceLy, ViewUtil.dpToPx(this, 25));
        showPopupText(
                getString(R.string.time_limit),
                getResources().getColor(R.color.redIndentity),
                2000
        );
        mainAnimationView.setAnimation("Tongue.json");
        mainAnimationView.setVisibility(View.VISIBLE);
        AnimationUtil.startScaleShowAnimation(mainAnimationView);
        mainAnimationView.playAnimation();

        interfaceLy.postDelayed(new Runnable() {
            @Override
            public void run() {
                AnimationUtil.startScaleHideAnimation(mainAnimationView);
                mainAnimationView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mainAnimationView.cancelAnimation();
                        mainAnimationView.setVisibility(View.GONE);
                    }
                }, 250);
                startNextRound();
            }
        }, 2000);
    }

    public void startNextRound() {
        round++;
        resetGameVars();
        resetView();

        setNextTurnPlayer();
        setBottomViews();
        sendMyInformation();
        startQuestionSelect();
    }

    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_single_player, R.id.button_single_player_2
    };

    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    private void clearPlayerView() {
        for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
            pScoreViews[i].setText("0");
            pDrawScoreViews[i].setText("0");
        }
    }

    private void setPlayerViews() {
        for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
            if(mParticipants == null || i >= mParticipants.size()){
                pLayouts[i].setVisibility(View.GONE);
            }else{
                pLayouts[i].setVisibility(View.VISIBLE);
            }
        }

        if(mParticipants != null) {

            for (int i = 0; i < mParticipants.size(); i++) {
                Participant p = mParticipants.get(i);
                pNameViews[i].setText(p.getDisplayName());
            }

        }
    }

    private void setBottomViews() {
        if(isMyTurn()) {

            answerBtn.setEnabled(false);
            AnimationUtil.startScaleHideAnimation(answerBtn);

            final AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(
                    ObjectAnimator.ofFloat(drawingToolView, "translationY",
                            drawingToolView.getTranslationY(), deviceHeight - ViewUtil.dpToPx(this, 50))
                            .setDuration(250)
            );
            animSet.setInterpolator(new FastOutSlowInInterpolator());
            animSet.start();

            AnimationUtil.startToBottomDisappearAnimation(inputLy, ViewUtil.dpToPx(this, 50));

        }else {

            answerBtn.setEnabled(true);
            AnimationUtil.startScaleShowAnimation(answerBtn);

            final AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(
                    ObjectAnimator.ofFloat(drawingToolView, "translationY",
                            drawingToolView.getTranslationY(), deviceHeight)
                            .setDuration(250)
            );
            animSet.setInterpolator(new FastOutSlowInInterpolator());
            animSet.start();

            AnimationUtil.startFromBottomSlideAppearAnimation(inputLy, ViewUtil.dpToPx(this, 50));

        }
    }

    private void showChatMessage(String senderId, GameMessage msg) {
        if(playerMap.containsKey(senderId)) {
            Chat chat = new Chat();
            chat.player = playerMap.get(senderId);
            chat.type = msg.type;
            chat.message = msg.text;
            chatAdapter.addChat(chat);
        }
    }

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }

    void switchToMainScreen() {
        currentTurnPlayerIndex = 0;
        currentTurnPlayerId = null;
        chatAdapter.clear();
        round = 1;

        for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
            pScoreViews[i].setText("0");
            pDrawScoreViews[i].setText("0");
        }

        if(questionDialog != null && questionDialog.isShowing()) {
            questionDialog.dismiss();
        }

        if(evaluateDialog != null && evaluateDialog.isShowing()) {
            evaluateDialog.dismiss();
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            switchToScreen(R.id.screen_main);
        }
        else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    /*
     * 통신 섹션 : 게임 네트워크 프로토콜 구현 메소드
     */

    void broadcastScore(boolean reliable, byte[] mMsgBuf) {
        if (!mMultiplayer || mRoomId == null)
            return; // playing single-player mode

        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            if (reliable) {
                // final score notification must be sent via reliable message
                Games.RealTimeMultiplayer.sendReliableMessage(
                        mGoogleApiClient,
                        null,
                        mMsgBuf,
                        mRoomId,
                        p.getParticipantId()
                );
            } else {
                // it's an interim score notification, so we can use unreliable
                Games.RealTimeMultiplayer.sendUnreliableMessage(
                        mGoogleApiClient,
                        mMsgBuf,
                        mRoomId,
                        p.getParticipantId()
                );
            }
        }
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        GameMessage msg = (GameMessage) ByteUtil.toObject(buf);
        Log.d(TAG, "Message received: " + msg.type);

        if(msg.type >= 0 && msg.type <= 3) {

            if(msg.pNum == pNum) {
                drawPoint(msg);
            }else {
                savePoint(msg);
            }

            while(savedPNumMap.containsKey(pNum)) {
                drawPoint(savedPNumMap.remove(pNum));
            }

        }else if(msg.type >= 100 && msg.type <= 101) {

            String senderId = rtm.getSenderParticipantId();
            showChatMessage(senderId, msg);

            if(msg.type == 101) {

                Player p = playerMap.get(senderId);

                if(msg.pNum == 1) {
                    correctAnswer(p);
                }else {
                    incorrectAnswer(p);
                }

            }

        }else if(msg.type == 200) {

            Question.selectQuestion((int)msg.x, (int)msg.y);
            setQuestion();
            if(questionDialog != null) {
                questionDialog.selectedQuestion(msg.time);
            }

        }else if(msg.type == 203) {

            int evaluateNum = msg.pNum;
            if(evaluateDialog != null) {
                evaluateDialog.receiveEvaluate(evaluateNum);
            }

        }else if(msg.type == 204) {

            if(evaluateDialog != null) {
                evaluateDialog.finishEvaluate();
            }

        }else if(msg.type == 205) {

            if(gameStatus != GAME_STATUS_CORRECT_ANSWER) {
                timeLimited();
            }

        }
    }

    private void sendMyInformation() {

    }

    private void sendInputMessage() {
        String text = input.getText().toString().trim();

        if(!TextUtils.isEmpty(text) && gameStatus == GAME_STATUS_DRAWING) {

            GameMessage msg;

            if(chatMode == CHAT_NORMAL) {

                msg = makeMessage(100, text, 0, 0, 0, System.currentTimeMillis());
                broadcastScore(false, ByteUtil.toByteArray(msg));

            }else{

                boolean isCorrected = text.equals(Question.getCurrentQuestion());

                msg = makeMessage(101, text, isCorrected ? 1 : 0, 0, 0, System.currentTimeMillis());
                broadcastScore(true, ByteUtil.toByteArray(msg));

                setChatMode(CHAT_NORMAL);
                hideKeyPad();

                if(isCorrected) {
                    correctAnswer(playerMap.get(mMyId));
                }else {
                    incorrectAnswer(playerMap.get(mMyId));
                }

            }

            showChatMessage(mMyId, msg);
            input.setText("");
        }
    }

    private void hideKeyPad() {
        input.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    /*
     * 기타 섹션
     */

    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static GameMessage makeMessage(int type,
                                          String text,
                                          int pNum,
                                          float x,
                                          float y,
                                          long time) {
        GameMessage message = new GameMessage();
        message.type = type;
        message.text = text;
        message.pNum = pNum;
        message.x = x;
        message.y = y;
        message.time = time;
        return message;
    }

    private void drawPoint(GameMessage msg) {
        pNum++;

        switch (msg.type) {
            case 0:
                inkView.onActionDown(
                        inkViewWidth * msg.x,
                        inkViewHeight * msg.y,
                        msg.time
                );
                break;
            case 1:
                inkView.onActionMove(
                        inkViewWidth * msg.x,
                        inkViewHeight * msg.y,
                        msg.time
                );
                break;
            case 2:
                inkView.onActionUp();
                break;
            case 3:
                inkView.setColor(Integer.parseInt(msg.text));
                break;
            case 4:
                inkView.setTool(Integer.parseInt(msg.text));
                break;
            default:
                break;
        }
    }

    private void savePoint(GameMessage msg) {
        savedPNumMap.put(msg.pNum, msg);
    }

    public void showPopupText(String string, int color, long delay){
        popupText.setText(string);
        popupText.setTextColor(color);
        AnimationUtil.startScaleShowAnimation(popupText);
        popupText.postDelayed(new Runnable() {
            @Override
            public void run() {
               AnimationUtil.startScaleHideAnimation(popupText);
            }
        }, delay);
    }

    private int getPlayerIndex(Player player) {
        for (int i = 0; i < mParticipants.size(); i++) {
            if(mParticipants.get(i).getParticipantId().equals(player.id)){
                return i;
            }
        }
        return -1;
    }

    private void setNextTurnPlayer() {
        if(currentTurnPlayerId == null) {
            currentTurnPlayerIndex = 0;
        }else {
            currentTurnPlayerIndex = (currentTurnPlayerIndex + 1) % mParticipants.size();
        }
        currentTurnPlayerId = mParticipants.get(currentTurnPlayerIndex).getParticipantId();
    }

    private boolean isMyTurn() {
        try{
            return currentTurnPlayerId.equals(mMyId);
        }catch (Exception ignored){}
        return true;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
