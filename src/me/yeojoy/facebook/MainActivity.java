
package me.yeojoy.facebook;

import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

import me.yeojoy.facebook.R;
import me.yeojoy.facebook.util.RoundingDrawable;
import my.lib.MyLog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String SP_NAME = "faceBoOk_TesT";
    private static final String KEY_ACCESS_TOKEN = "at";
    private static final String KEY_EXPIRE_TIME = "e_time";
    private static final String KEY_PERMISSION = "permiSSioNs";
    
    private ImageView mIvProfile;
//    private ProfilePictureView mIvProfile;
    private TextView mTvText;

    private Button mBtnLogin, mBtnLoginWithAT, mBtnLoginWithPerm;
    private Button mBtnFacebookLike, mBtnFacebookInvite, mBtnFacebookPublish;
    
    private Context mContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        
        mIvProfile = (ImageView) findViewById(R.id.iv_profile_image);
        
        mTvText = (TextView) findViewById(R.id.tv_text);
        
        mUiFacebookHelper = new UiLifecycleHelper(this, mStatusCallback);
        mUiFacebookHelper.onCreate(savedInstanceState);
        
//        setProfileImage();
        
        mBtnLogin = (Button) findViewById(R.id.btn_facebook_auth);
        mBtnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = Session.getActiveSession();
                if (session.isOpened()) {
                    session.closeAndClearTokenInformation();
                } else {
                    Session.openActiveSession(MainActivity.this, true, mStatusCallback);
                }
            }
        });
        
        mBtnLoginWithAT = (Button) findViewById(R.id.btn_facebook_at);
        mBtnLoginWithAT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = Session.getActiveSession();
                if (session.isOpened()) {
                    session.closeAndClearTokenInformation();
                } else {
                    Session.openActiveSessionWithAccessToken(mContext, getAccessToken(), mStatusCallback);
                }
            }
        });
        
        mBtnLoginWithPerm = (Button) findViewById(R.id.btn_facebook_with_permission);
        mBtnLoginWithPerm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = Session.getActiveSession();
                if (session.isOpened()) {
                    session.closeAndClearTokenInformation();
                } else {
                    loginFacebookWithPermission();
                }
            }
        });
        
        mBtnFacebookLike = (Button) findViewById(R.id.btn_facebook_like);
        mBtnFacebookLike.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLog.i("yeojoy", "like button click");
//                postFeedTopic();
                postLikeTopic();
            }
        });
        
        mBtnFacebookInvite = (Button) findViewById(R.id.btn_facebook_invite);
        mBtnFacebookInvite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteMyFriends();
            }
        });
        
        mBtnFacebookPublish = (Button) findViewById(R.id.btn_facebook_publish);
        mBtnFacebookPublish.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                publishMyFeed();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mUiFacebookHelper.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mUiFacebookHelper.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUiFacebookHelper.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    private void setProfileImage(final String id) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = saveBitmapFbProfileImg(id);
                final RoundingDrawable rd = new RoundingDrawable(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIvProfile.setImageDrawable(rd);
                    } 
                });
            } 
        };
        new Thread(runnable).start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        mUiFacebookHelper.onActivityResult(requestCode, resultCode, data);
    }

    
    private void saveAccessToken(Session session) {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (session == null) {
            MyLog.i("saveAccessToken(). clear SharedPreferences");
            
            editor.putString(KEY_ACCESS_TOKEN, null);
            editor.putLong(KEY_EXPIRE_TIME, Long.MIN_VALUE);
            editor.putStringSet(KEY_PERMISSION, null);
            
        } else {
            MyLog.i("saveAccessToken(). Save access token information");
            List<String> permissions = session.getPermissions();
            for (String p : permissions) MyLog.i("yeojoy", "Permission : " + p);
            
            Set<String> accessTokenPermissions = new HashSet<String>();
            accessTokenPermissions.addAll(session.getPermissions());
            
            editor.putString(KEY_ACCESS_TOKEN, session.getAccessToken());
            editor.putLong(KEY_EXPIRE_TIME, session.getExpirationDate().getTime());
            editor.putStringSet(KEY_PERMISSION, accessTokenPermissions);
            
        }
        editor.apply();
    }
    
    /**
     * SharedPreferences에 저장된 Accesstoken, expiration time, permission을 사용해서
     * AccessToken obj를 생성한다.
     */
    private AccessToken getAccessToken() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String accessToken = sp.getString(KEY_ACCESS_TOKEN, null);
        Date expirationTime = new Date(sp.getLong(KEY_EXPIRE_TIME, 0));
        List<String> permissions = new ArrayList<String>();
        permissions.addAll(sp.getStringSet(KEY_PERMISSION, null));

        if (accessToken == null) return null;
        
//        return AccessToken.createFromExistingAccessToken(accessToken, expirationTime, null, null, permissions); 
        return AccessToken.createFromExistingAccessToken(accessToken, null, null, null, null); 
    }
    
    private void clearUserInfo() {
        mBtnLogin.setText("로그인");
        mBtnLoginWithAT.setText("로그인_AT");
        mBtnLoginWithPerm.setText("로그인_permission");
        
        mTvText.setText("");
        mIvProfile.setImageDrawable(null);
        
        mUser = null;
    }
    
    /* FACEBOOK */
    private GraphUser mUser;
    private UiLifecycleHelper mUiFacebookHelper;
    private Session.StatusCallback mStatusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened()) {
                MyLog.d("session open");

                if (!hasEmailPermission())
                    requestEmailPermission();
                
                mBtnLogin.setText("로그아웃");
                mBtnLoginWithAT.setText("로그아웃_AT");
                mBtnLoginWithPerm.setText("로그아웃_permission");
                
                saveAccessToken(session);
                String text = mTvText.getText().toString();
                
                if (mUser == null || text.contains("null")) {
                    Request.executeMeRequestAsync(session, new GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                
//                              mIvProfile.setProfileId(user.getId());
                                setProfileImage(user.getId());
                                mUser = user;
                                
                                String email = (String) user.asMap().get("email");
                                
                                StringBuilder sb = new StringBuilder();
                                sb.append(mUser.getName()).append("! Hi!").append("   ");
                                sb.append(email);
                                
                                mTvText.setText(sb);
                            }
                        }
                    });
                }
                
            } else
                MyLog.d("session doesn't open");
                
            
            if (session.isClosed()) {
                MyLog.d("session closes");

                clearUserInfo();
                
            } else {
                MyLog.d("session doesn't close");
                
            }
        }
    };
    
    /** 
     * 페이스북 프로필 이미지 Bitmap으로 변환
     * @param GraphUser obj.getId()
     * @return
     */
    public Bitmap saveBitmapFbProfileImg(String uid) {
        Bitmap fbImg = null;
        URL imgUrl = null;
        StringBuilder url = new StringBuilder();
        // pictur size. small, normal, large, square
        url.append("http://graph.facebook.com/").append(uid).append("/picture?type=large");
        
        try {
            imgUrl = new URL(url.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        
        try {
            fbImg = BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return fbImg;
    }
    
    private void postLikeTopic() {
        if (!hasPublishPermission()) {
            requestPublishPermission();
            return;
        }
        
        Session session = Session.getActiveSession();
        if (session.isOpened() && mUser != null) {
            
            // me/og.likes
            Bundle params = new Bundle();
            params.putString("object", "http://yeojoy.tistory.com/18");
            
            Request request = new Request(session, "me/og.likes", params, HttpMethod.POST);
            request.setCallback(new Request.Callback() {
                
                @Override
                public void onCompleted(Response response) {
                    MyLog.d(response.toString());
                }
            });
            request.executeAsync();
            
        }
    }
    
    private void postFeedTopic() {
        MyLog.i("yeojoy", "postFeedTopic()");
        if (!hasPublishPermission()) {
            MyLog.i("yeojoy", "there isn't publish_actions permission");
            requestPublishPermission();
            return;
        }
        
        Session session = Session.getActiveSession();
        if (session.isOpened() && mUser != null) {
            MyLog.i("yeojoy", "session open and request api for post");
            Bundle params = new Bundle();
            params.putString("object", "http://yeojoy.tistory.com/18");
            
            Request request = new Request(session, "me/og.posts", params, HttpMethod.POST);
            request.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    MyLog.d("response is not null");
                    MyLog.d(response.toString());
                }
            });
            
        }
        
    }
    
    private void loginFacebookWithPermission() {
        Session session = new Session.Builder(mContext).build();
        Session.setActiveSession(session);
        
        OpenRequest openRequest = new Session.OpenRequest((Activity) mContext);
        // SessionLoginBehavior type
        // SUPPRESS_SSO : login on facebook webview dialog
        // SSO_ONLY : login on facebook native app
        // SSO_WITH_FALLBACK : login alternatively, bur first way is native app.
        // publish_actions 과 같은 권한을 줄 때에는 SUPPRESS_SSO를 사용해야 한다.
        openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
        openRequest.setCallback(mStatusCallback);
        
        List<String> permissions = new ArrayList<String>();
        permissions.add("basic_info");
//        permissions.add("email");
        openRequest.setPermissions(permissions);
        
        openRequest.setDefaultAudience(SessionDefaultAudience.FRIENDS);
        
        session.openForPublish(openRequest);
    }
    
    private void inviteMyFriends() {
        if (!hasPublishPermission()) {
            requestPublishPermission();
            return;
        }
        
        Bundle params = new Bundle();
        params.putString("message", "이 거 한 번 좝쏴봐");
        
        WebDialog requestDialog = new WebDialog.RequestsDialogBuilder(mContext, Session.getActiveSession(), params).build();
        requestDialog.setTitle("엠하이다크");
        requestDialog.setCanceledOnTouchOutside(true);
        requestDialog.setCancelable(true);
        requestDialog.setOnShowListener(new OnShowListener() {
            
            @Override
            public void onShow(DialogInterface dialog) {
                MyLog.d("Dialog is showing");
            }
        });
        requestDialog.setOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, FacebookException error) {
                if (error != null) {
                    if (error instanceof FacebookOperationCanceledException) {
                        Toast.makeText(mContext,  
                            "Request cancelled", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext,  
                            "Network Error", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    final String requestId = values.getString("request");
                    if (requestId != null) {
                        Toast.makeText(mContext,  
                            "Request sent >> " + requestId,  
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext,  
                            "Request cancelled", 
                            Toast.LENGTH_SHORT).show();
                    }
                }   
            }
        });
        requestDialog.show();
        
    }
    
    private void publishMyFeed() {
        if (!hasPublishPermission()) {
            requestPublishPermission();
            return;
        }
        
        Bundle params = new Bundle();
        params.putString("name", "엠하이닥");
        params.putString("caption", "caption은 이렇게 나옵니다..");
        params.putString("description", "앱 publish Test. 아 대박...");
        params.putString("link", "https://mhidoc.co.kr/install");
        params.putString("picture", "https://lh5.ggpht.com/zvPjOA48KB6ezycP58epMVYYaj8nAD0bnfjp3Pe_7aF2Lx9LiTR2ysjbTwyDUH7GQw=w300-rw");
        WebDialog feedDialog = (
            new WebDialog.FeedDialogBuilder(mContext,
                Session.getActiveSession(),
                params))
            .setOnCompleteListener(new OnCompleteListener() {

                @Override
                public void onComplete(Bundle values,
                    FacebookException error) {
                    if (error == null) {
                        // When the story is posted, echo the success
                        // and the post Id.
                        final String postId = values.getString("post_id");
                        if (postId != null) {
                            Toast.makeText(mContext,
                                "Posted story, id: "+postId,
                                Toast.LENGTH_SHORT).show();
                        } else {
                            // User clicked the Cancel button
                            Toast.makeText(mContext, 
                                "Publish cancelled", 
                                Toast.LENGTH_SHORT).show();
                        }
                    } else if (error instanceof FacebookOperationCanceledException) {
                        // User clicked the "x" button
                        Toast.makeText(mContext, 
                            "Publish cancelled", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        // Generic, ex: network error
                        Toast.makeText(mContext, 
                            "Error posting story", 
                            Toast.LENGTH_SHORT).show();
                    }
                }

            })
            .build();
        feedDialog.setCancelable(true);
        feedDialog.setCanceledOnTouchOutside(true);
        feedDialog.show();
    }
    
    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }
    
    private void requestPublishPermission() {
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
//            List<String> permissions = new ArrayList<String>();
//            permissions.add("publish_actions");
            session.requestNewPublishPermissions(
                    new Session.NewPermissionsRequest((Activity)mContext, Arrays.asList("publish_actions")));
        }
    }
    
    private boolean hasEmailPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("email");
    }
    
    private void requestEmailPermission() {
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
//            List<String> permissions = new ArrayList<String>();
//            permissions.add("email");
            session.requestNewPublishPermissions(
                    new Session.NewPermissionsRequest((Activity)mContext, Arrays.asList("email")));
        }
    }
}
