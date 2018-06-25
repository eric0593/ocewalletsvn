package com.idea.jgw.ui.user;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.idea.jgw.App;
import com.idea.jgw.R;
import com.idea.jgw.RouterPath;
import com.idea.jgw.api.OkhttpApi;
import com.idea.jgw.api.retrofit.ServiceApi;
import com.idea.jgw.bean.BaseResponse;
import com.idea.jgw.bean.UserInfo;
import com.idea.jgw.dialog.ChoosePhotoDialog;
import com.idea.jgw.dialog.LoadingDialog;
import com.idea.jgw.ui.BaseActivity;
import com.idea.jgw.ui.main.fragment.MineFragment;
import com.idea.jgw.utils.SPreferencesHelper;
import com.idea.jgw.utils.baserx.RxSubscriber;
import com.idea.jgw.utils.common.CommonUtils;
import com.idea.jgw.utils.common.DialogUtils;
import com.idea.jgw.utils.common.MToast;
import com.idea.jgw.utils.common.MyLog;
import com.idea.jgw.utils.common.ShareKey;
import com.idea.jgw.utils.common.SharedPreferenceManager;
import com.idea.jgw.utils.glide.GlideApp;
import com.joker.annotation.PermissionsCustomRationale;
import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsNonRationale;
import com.joker.api.Permissions4M;
import com.socks.okhttp.plus.OkHttpProxy;
import com.socks.okhttp.plus.listener.UploadListener;
import com.socks.okhttp.plus.model.Progress;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.idea.jgw.api.OkhttpApi.BASE_HOST;

/**
 * 个人信息
 */
@Route(path = RouterPath.USER_INFO_ACTIVITY)
public class UserInfoActivity extends BaseActivity implements ChoosePhotoDialog.OnChooseListener {
    //调用系统相机请求码
    public static final int DO_CAMERA_REQUEST = 100;
    //调用系统相册请求码
    public static final int OPEN_SYS_ALBUMS_REQUEST = 101;
    //调用系统截图请求码
    public static final int SYS_CROP_REQUEST = 102;
    private static final int CAMERA_CODE = 11;
    private static final int ABLUM_STORAGE_CODE = 12; //相册访问sd权限
    private static final int TAKEPHOTO_STORAGE_CODE = 13; //拍照访问sd权限
    private static final int EXTERNAL_STORAGE_CODE = 14; //拍照访问sd权限
    private static final int UPDATE_NICKNAME = 15;

    @BindView(R.id.btn_of_back)
    Button btnOfBack;
    @BindView(R.id.tv_of_title)
    TextView tvOfTitle;
    @BindView(R.id.iv_photo)
    ImageView ivPhoto;
    @BindView(R.id.tv_nickname)
    TextView tvNickname;
    @BindView(R.id.ll_nickname)
    LinearLayout llNickname;
    @BindView(R.id.tv_phone)
    TextView tvPhone;
    @BindView(R.id.ll_phone)
    LinearLayout llPhone;

    ChoosePhotoDialog choosePhotoDialog;
    private String userPhotoPath;
    UserInfo userInfo;
    private String nickname;
    private String face;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_user_info;
    }

    @Override
    public void initView() {
        if (getIntent().hasExtra("userInfo")) {
            userInfo = getIntent().getParcelableExtra("userInfo");
        }
        if (userInfo != null) {
            nickname = userInfo.getNickname();
            tvNickname.setText(nickname);
            face = userInfo.getFace();
            Glide.with(this).load(BASE_HOST + face).apply(RequestOptions.circleCropTransform()).into(ivPhoto);
        }
        tvPhone.setText(SPreferencesHelper.getInstance(App.getInstance()).getData(ShareKey.KEY_OF_PHONE, "").toString());
    }

    @OnClick({R.id.btn_of_back, R.id.iv_photo, R.id.ll_nickname, R.id.ll_phone})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_of_back:
                finish();
                break;
            case R.id.iv_photo:
                showPhotoChooseDialog();
                break;
            case R.id.ll_nickname:
                ARouter.getInstance().build(RouterPath.NIKENAME_ACTIVITY2).withString("nickname", nickname).navigation(this, UPDATE_NICKNAME);
                break;
            case R.id.ll_phone:
//                ARouter.getInstance().build(RouterPath.SECURITY_MANAGER_ACTIVITY).navigation();
                break;
        }
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("nickname", nickname);
        intent.putExtra("face", face);
        setResult(RESULT_OK,intent);
        super.finish();
    }

    private void requestCameraPermission() {
        Permissions4M.get(this)
                // 是否强制弹出权限申请对话框，建议设置为 true，默认为 true
                .requestForce(true)
                // 是否支持 5.0 权限申请，默认为 false
                .requestUnderM(true)
                // 权限，单权限申请仅只能填入一个
                .requestPermissions(Manifest.permission.CAMERA)
                // 权限码
                .requestCodes(CAMERA_CODE)
                // 如果需要使用 @PermissionNonRationale 注解的话，建议添加如下一行
                // 返回的 intent 是跳转至**系统设置页面**
                .requestPageType(Permissions4M.PageType.MANAGER_PAGE)
                // 返回的 intent 是跳转至**手机管家页面**
                // .requestPageType(Permissions4M.PageType.ANDROID_SETTING_PAGE)
                .request();
    }

    @PermissionsGranted(CAMERA_CODE)
    public void cameraGranted() {

        requestStoragePermission(TAKEPHOTO_STORAGE_CODE);
    }

    private void requestStoragePermission(int requestCode) {
        Permissions4M.get(this)
                // 是否强制弹出权限申请对话框，建议设置为 true，默认为 true
                .requestForce(true)
                // 是否支持 5.0 权限申请，默认为 false
                .requestUnderM(true)
                // 权限，单权限申请仅只能填入一个
                .requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                // 权限码
                .requestCodes(requestCode)
                // 如果需要使用 @PermissionNonRationale 注解的话，建议添加如下一行
                // 返回的 intent 是跳转至**系统设置页面**
                .requestPageType(Permissions4M.PageType.MANAGER_PAGE)
                // 返回的 intent 是跳转至**手机管家页面**
                // .requestPageType(Permissions4M.PageType.ANDROID_SETTING_PAGE)
                .request();
    }

    public void showPhotoChooseDialog() {
        if (choosePhotoDialog == null) {
            choosePhotoDialog = new ChoosePhotoDialog(this, this);
        }
        choosePhotoDialog.show();
    }

    @PermissionsDenied(CAMERA_CODE)
    public void cameraDenied() {
        MToast.showToast("相机权限授权失败！");
    }

    @PermissionsDenied(EXTERNAL_STORAGE_CODE)
    public void storageDenied() {
        MToast.showToast("SD卡权限授权失败！");
    }

    @PermissionsCustomRationale({TAKEPHOTO_STORAGE_CODE, ABLUM_STORAGE_CODE, CAMERA_CODE})
    public void cameraCustomRationale(final int code) {
        switch (code) {
            case TAKEPHOTO_STORAGE_CODE:
            case ABLUM_STORAGE_CODE:
                DialogUtils.showAlertDialog(this, "SD卡权限申请：\n我们需要您开启SD权限，一边访问上传头像", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Permissions4M.get(UserInfoActivity.this)
                                .requestOnRationale()
                                .requestPermissions(Manifest.permission.CAMERA)
                                .requestCodes(code)
                                .request();
                    }
                });
                break;
            case CAMERA_CODE:
                DialogUtils.showAlertDialog(this, "相机权限申请：\n我们需要您开启相机信息权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Permissions4M.get(UserInfoActivity.this)
                                .requestOnRationale()
                                .requestPermissions(Manifest.permission.CAMERA)
                                .requestCodes(CAMERA_CODE)
                                .request();
                    }
                });
                break;
        }
    }

    @PermissionsNonRationale({TAKEPHOTO_STORAGE_CODE, ABLUM_STORAGE_CODE, CAMERA_CODE})
    public void non(int code, final Intent intent) {
        switch (code) {
            case TAKEPHOTO_STORAGE_CODE:
            case ABLUM_STORAGE_CODE:
                DialogUtils.showAlertDialog(this, "sd卡权限申请：\n我们需要您开启读SD卡权限，以便上传照片", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(intent);
                    }
                });
                break;
            case CAMERA_CODE:
                DialogUtils.showAlertDialog(this, "读取相机权限申请：\n我们需要您开启读取相机权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(intent);

                    }
                });
                break;
        }
    }

    @Override
    public void choose(int which) {
        switch (which) {
            case ChoosePhotoDialog.ALBUM:
                requestStoragePermission(ABLUM_STORAGE_CODE);
                break;
            case ChoosePhotoDialog.CANCEL:
                choosePhotoDialog.dismiss();
                break;
            case ChoosePhotoDialog.TAKE_PHOTO:
                requestCameraPermission();
                break;
        }
    }

    @PermissionsGranted(TAKEPHOTO_STORAGE_CODE)
    public void takePhoto() {
        userPhotoPath = CommonUtils.doCamra(this, "userPhotoPath.jpg", DO_CAMERA_REQUEST);
        choosePhotoDialog.dismiss();
    }

    @PermissionsGranted(ABLUM_STORAGE_CODE)
    public void pickPhoto() {
        userPhotoPath = CommonUtils.openSysPick(this, "userPhotoPath.jpg", OPEN_SYS_ALBUMS_REQUEST);
        choosePhotoDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String resultKey = "";
            switch (requestCode) {
                case DO_CAMERA_REQUEST:
                    CommonUtils.cropImageUri(this, userPhotoPath, SYS_CROP_REQUEST);
                    break;
                case SYS_CROP_REQUEST:
                    updateUserPhoto(userPhotoPath);
                    break;
                case OPEN_SYS_ALBUMS_REQUEST:
                    if (data != null) {
                        MyLog.e("data.getData()" + data.getData());
                        MyLog.e(data.getData().toString());
                        userPhotoPath = CommonUtils.getRealPathFromUri(this, data.getData());
                        updateUserPhoto(userPhotoPath);
                    } else {
                        MToast.showToast("图片损坏，请重新选择");
                    }
                    break;
                case UPDATE_NICKNAME:
                    if(data.hasExtra("nickname")) {
                        nickname = data.getStringExtra("nickname");
                        tvNickname.setText(nickname);
                    }
                    break;
            }
        }
    }

    public void updateUserPhoto2(final String fileName) {
        File file = new File(Environment.getExternalStorageDirectory(), "HldImage/userPhotoPath.jpg");
        Map<String, Object> map = new HashMap<>();
        map.put("token", "6fd95490e77cdf77c9c8162641d2cb6c");
        RequestBody body1 = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("avatarByte", file.getName(), body1);
        String token = SPreferencesHelper.getInstance(App.getInstance()).getData(ShareKey.KEY_OF_SESSION, "").toString();
        ServiceApi.getInstance().getApiService().updatePhoto(token, part)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RxSubscriber<BaseResponse>(this, getResources().getString(R.string.loading), true) {
                               @Override
                               protected void _onNext(BaseResponse baseResponse) {
                                   MToast.showToast(baseResponse.getData().toString());
                               }

                               @Override
                               protected void _onError(String message) {
                                   MToast.showToast(message);
                               }
                           }
                );
    }

    /**
     * <p>上传头像都附件服务器</p>
     *
     * @param fileName 要上传的文件名
     */
    public void updateUserPhoto(final String fileName) {
//        updateUserPhoto2(fileName);
        File file = new File(fileName);
//        File file = new File(Environment.getExternalStorageDirectory(), "HldImage/userPhotoPath.jpg");

        String token = SPreferencesHelper.getInstance(App.getInstance()).getData(ShareKey.KEY_OF_SESSION, "").toString();
        OkhttpApi.updatePhoto(token, file, new UploadListener() {
            @Override
            public void onSuccess(String data) {
                BaseResponse baseResponse = JSON.parseObject(data, BaseResponse.class);
                if (baseResponse.getCode() == 200) {
                    face = baseResponse.getData().toString();
                    Glide.with(UserInfoActivity.this).load(BASE_HOST + face).apply(RequestOptions.circleCropTransform()).into(ivPhoto);
                }
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onUIProgress(Progress progress) {
            }

            @Override
            public void onUIStart() {
                LoadingDialog.showDialogForLoading(UserInfoActivity.this);
            }

            @Override
            public void onUIFinish() {
                LoadingDialog.cancelDialogForLoading();
            }
        });
    }

    /**
     * 更新修改后的控件背景
     *
     * @param view 要修改的空间
     * @param url  图片路径
     */
    private void updateImageView(final View view, final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(UserInfoActivity.this).load(userPhotoPath).into((ImageView) view);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        Permissions4M.onRequestPermissionsResult(this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}