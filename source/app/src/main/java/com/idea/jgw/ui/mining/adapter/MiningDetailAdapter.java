package com.idea.jgw.ui.mining.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.idea.jgw.R;
import com.idea.jgw.bean.MiningCoinData;
import com.idea.jgw.ui.BaseRecyclerAdapter;
import com.idea.jgw.utils.common.DateUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by idea on 2018/5/16.
 */

public class MiningDetailAdapter extends BaseRecyclerAdapter<MiningCoinData> {

    @Override
    public RecyclerView.ViewHolder onCreate(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_of_mining_detail, parent, false);
        DigitalCurrencyListHolder holder = new DigitalCurrencyListHolder(view);
        return holder;
    }

    @Override
    public void onBind(RecyclerView.ViewHolder viewHolder, int realPosition, MiningCoinData data) {
        int type = data.getType();
        String prefix = "";
        if(data.getType() == 1) {
            prefix = "+";
        } else if(type == 2) {
            prefix = "-";
        }
        ((DigitalCurrencyListHolder)viewHolder).tvOfMiningNumber.setText(prefix + String.valueOf(data.getNum()));
        ((DigitalCurrencyListHolder)viewHolder).tvOfMiningTime.setText(DateUtils.longToString(data.getTime(), DateUtils.DTIME_STYLE1));
    }

    class DigitalCurrencyListHolder extends Holder {

        @BindView(R.id.tv_of_mining_num)
        TextView tvOfMiningNumber;
        @BindView(R.id.tv_of_mining_time)
        TextView tvOfMiningTime;

        public DigitalCurrencyListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}