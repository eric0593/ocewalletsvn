package com.idea.jgw.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.idea.jgw.App;
import com.idea.jgw.R;
import com.idea.jgw.RouterPath;
import com.idea.jgw.api.retrofit.ServiceApi;
import com.idea.jgw.bean.BaseResponse;
import com.idea.jgw.bean.LoginRequest;
import com.idea.jgw.logic.btc.BtcWalltUtils;
import com.idea.jgw.ui.BaseActivity;
import com.idea.jgw.utils.SPreferencesHelper;
import com.idea.jgw.utils.baserx.RxSubscriber;
import com.idea.jgw.utils.common.CommonUtils;
import com.idea.jgw.utils.common.MToast;
import com.idea.jgw.utils.common.ShareKey;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.Observers;
import rx.schedulers.Schedulers;

@Route(path = RouterPath.LOGIN_ACTIVITY)
public class LoginActivity extends BaseActivity {

    @BindView(R.id.btn_of_back)
    Button btnOfBack;
    @BindView(R.id.tv_of_title)
    TextView tvOfTitle;
    @BindView(R.id.et_of_nickname)
    EditText etOfPhone;
    @BindView(R.id.iBtn_of_delete)
    ImageButton iBtnOfDelete;
    @BindView(R.id.et_of_pwd)
    EditText etOfPwd;
    @BindView(R.id.iBtn_of_show_pwd)
    ImageButton iBtnOfShowPwd;
    @BindView(R.id.tv_of_forget_pwd)
    TextView tvOfForgetPwd;
    @BindView(R.id.tv_of_register)
    TextView tvOfRegister;
    @BindView(R.id.btn_of_update)
    Button btnOfLogin;

    Subscription loginSubscription;

    public static final String EXTRA_USER = "LoginActivity_EXTRA_USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    public void initView() {
        btnOfBack.setVisibility(View.GONE);
        tvOfTitle.setText(R.string.login);
        CommonUtils.setTextPwdInputType(etOfPwd);
    }

    @OnClick({R.id.btn_of_back, R.id.iBtn_of_delete, R.id.iBtn_of_show_pwd, R.id.tv_of_forget_pwd, R.id.tv_of_register, R.id.btn_of_update})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_of_back:
                finish();
                break;
            case R.id.iBtn_of_delete:
                etOfPhone.setText("");
                break;
            case R.id.iBtn_of_show_pwd:
                if(view.isSelected()) {
                    view.setSelected(false);
                    CommonUtils.setTextPwdInputType(etOfPwd);
                } else {
                    view.setSelected(true);
                    CommonUtils.setTextInputType(etOfPwd);
                }
                break;
            case R.id.tv_of_forget_pwd:
                ARouter.getInstance().build(RouterPath.GET_VERIFICATION_CODE_ACTIVITY).navigation();
                break;
            case R.id.tv_of_register:
                ARouter.getInstance().build(RouterPath.REGISTER_ACTIVITY).navigation();
                finish();
                break;
            case R.id.btn_of_update:



                if(App.isIsWalletDebug2){
                    ARouter.getInstance().build(RouterPath.MAIN_ACTIVITY)
                            .withString(EXTRA_USER,"18681591321")
                            .navigation();
                    return;
                }

                if(App.isWalletDebug)
                {
                    ARouter.getInstance().build(RouterPath.LOAD_OR_CREATE_WALLET_ACTIVITY)
                            .withString(EXTRA_USER,"18681591321")
                            .navigation();
                    return;
                }

              final  String phone = etOfPhone.getText().toString().trim();
                String pwd = etOfPwd.getText().toString().trim();
                if(TextUtils.isEmpty(pwd)) {
                    MToast.showToast(R.string.phone_is_null);
                } else if(TextUtils.isEmpty(pwd)) {
                    MToast.showToast(R.string.verify_code_is_null);
                } else {

                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setAccount(phone);
                    loginRequest.setPasswd(pwd);
                    loginSubscription = ServiceApi.getInstance().getApiService()
                            .login(loginRequest.getQueryMap())
                            .subscribeOn(Schedulers.io())
                            .flatMap(new Func1<BaseResponse, Observable<Boolean>>() {
                                @Override
                                public Observable<Boolean> call(final BaseResponse baseResponse) {
                                    return Observable.create(new Observable.OnSubscribe<Boolean>(){
                                        @Override
                                        public void call(Subscriber<? super Boolean> subscriber) {
                                            if (baseResponse.getCode() == 200) {
                                                SPreferencesHelper.getInstance(App.getInstance()).saveData(ShareKey.KEY_OF_SESSION, baseResponse.getData().toString());
                                                SPreferencesHelper.getInstance(App.getInstance()).saveData(ShareKey.KEY_OF_LOGIN, true);
                                                SPreferencesHelper.getInstance(App.getInstance()).saveData(ShareKey.KEY_OF_PHONE, phone);
                                                boolean hasWallet = BtcWalltUtils.hasSetupHDWallet();
                                                subscriber.onNext(hasWallet);
                                                subscriber.onCompleted();

                                            } else if (baseResponse.getCode() == 0) {
//                                                MToast.showToast(baseResponse.getData().toString());
                                                subscriber.onError(new Exception(baseResponse.getData().toString()));
                                            }
                                        }
                                    });
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(new RxSubscriber<Boolean>(this, getResources().getString(R.string.loading), true) {
                        @Override
                        protected void _onNext(Boolean hasWallet) {
                            if(!hasWallet) {
                                ARouter.getInstance().build(RouterPath.LOAD_OR_CREATE_WALLET_ACTIVITY)
                                        .withString(EXTRA_USER,phone)
                                        .navigation();
                            } else {
                                ARouter.getInstance().build(RouterPath.MAIN_ACTIVITY).navigation();
                                finish();
                            }
                            finish();
                        }

                        @Override
                        protected void _onError(String message) {
                            MToast.showToast(message);
                        }
                    }
                    );
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unSubscribe(loginSubscription);
    }
}