package com.idea.jgw.ui.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.idea.jgw.App;
import com.idea.jgw.R;
import com.idea.jgw.RouterPath;
import com.idea.jgw.common.Common;
import com.idea.jgw.logic.eth.data.TransactionDisplay;
import com.idea.jgw.logic.eth.utils.AddressNameConverter;
import com.idea.jgw.ui.BaseActivity;
import com.idea.jgw.utils.SPreferencesHelper;
import com.idea.jgw.utils.common.MyLog;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 交易详情页面
 */
@Route(path = RouterPath.TRANSACTION_DETAIL_ACTIVITY)
public class TransactionDetailActivity extends BaseActivity {

    public static final String EXTRA_DETAIL_OBJECT = "TransactionDetailActivity.EXTRA_DETAIL_OBJECT";
    public static final String EXTRA_COIN_TYPE = "TransactionDetailActivity.EXTRA_COIN_TYPE";

    @BindView(R.id.btn_of_back)
    Button btnOfBack;
    @BindView(R.id.tv_of_title)
    TextView tvOfTitle;
    @BindView(R.id.tv_send_time)
    TextView tvSendTime;
    @BindView(R.id.tv_transaction_number)
    TextView tvTransactionNumber;
    @BindView(R.id.tv_transaction_status)
    TextView tvTransactionStatus;
    @BindView(R.id.tv_send_address)
    TextView tvSendAddress;
    @BindView(R.id.tv_copy_send_address)
    TextView tvCopySendAddress;
    @BindView(R.id.tv_received_address)
    TextView tvReceivedAddress;
    @BindView(R.id.tv_copy_received_address)
    TextView tvCopyReceivedAddress;
    @BindView(R.id.tv_transaction_id)
    TextView tvTransactionId;
    @BindView(R.id.tv_commission)
    TextView tvCommission;
    @BindView(R.id.tv_chain_number)
    TextView tvChainNumber;
    @BindView(R.id.tv_send_label)
    TextView tvSendLabel;

    ClipboardManager mClipboardManager;


    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");

    @Autowired(name = EXTRA_COIN_TYPE)
    int coinType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_transcation_detail;
    }

    @Override
    public void initView() {
        tvOfTitle.setText(R.string.transaction_detail);

        //根据货币类型做处理
        coinType = getIntent().getIntExtra(EXTRA_COIN_TYPE, -1);
        if (coinType == Common.CoinTypeEnum.BTC.getIndex()) {

        } else if (coinType == Common.CoinTypeEnum.ETH.getIndex()) {

            BigDecimal bd = new BigDecimal(10).pow(18);
            DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
            df.setMaximumFractionDigits(18);

            TransactionDisplay td = (TransactionDisplay) getIntent().getSerializableExtra(EXTRA_DETAIL_OBJECT);
            tvTransactionId.setText(getResources().getString(R.string.transaction_id) + ":" + td.getTxHash());
            tvChainNumber.setText(getResources().getString(R.string.chain_number) + ":" + td.getBlock());
            tvSendTime.setText(sdf.format(new Date(td.getDate())));
            BigDecimal gas = new BigDecimal(td.getGasUsed() * td.getGasprice()).divide(bd);
            tvCommission.setText(getResources().getString(R.string.commission) + ":" + df.format(gas.doubleValue()));
            BigDecimal amount = new BigDecimal(td.getAmountNative()).divide(bd);
            tvTransactionNumber.setText(df.format(amount.doubleValue()));
            // amount > 0 表示接收，< 0表示发送
            if (td.getAmount() > 0) {
                tvSendLabel.setText(R.string.received);
                tvSendAddress.setText(td.getToAddress());
                tvReceivedAddress.setText(td.getFromAddress());
            }else{
                tvSendLabel.setText(R.string.send);
                tvReceivedAddress.setText(td.getToAddress());
                tvSendAddress.setText(td.getFromAddress());
            }
        } else if (coinType == Common.CoinTypeEnum.JGW.getIndex()) {
//            String address = SPreferencesHelper.getInstance(App.getInstance()).getData(Common.Eth.PREFERENCES_ADDRESS_KEY, "").toString();
            TransactionDisplay td = (TransactionDisplay) getIntent().getSerializableExtra(EXTRA_DETAIL_OBJECT);

        }
    }

    @OnClick({R.id.btn_of_back, R.id.tv_copy_send_address, R.id.tv_copy_received_address})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_of_back:
                finish();
                break;
            case R.id.tv_copy_send_address:
                copyText(tvSendAddress);
                break;
            case R.id.tv_copy_received_address:
                copyText(tvReceivedAddress);
                break;
        }
    }

    private void copyText(TextView textView) {
        if (mClipboardManager == null) {
            mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        }
        ClipData clipData = ClipData.newPlainText("address", textView.getText().toString().trim());
        mClipboardManager.setPrimaryClip(clipData);
    }
}
