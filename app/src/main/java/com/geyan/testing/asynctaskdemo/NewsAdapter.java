package com.geyan.testing.asynctaskdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * 继承BaseAdapter是最好的习惯
 * Created by geyan on 2016/7/24.
 */
public class NewsAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
    private List<NewsBean> mList;
    private Context mContext;
    private boolean mIsListViewIdle = true;
    private ListView mListView;

    public NewsAdapter(Context context, List<NewsBean> data, ListView listView) {
        this.mContext = context;
        this.mList = data;
        this.mListView = listView;
        mListView.setOnScrollListener(this);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list, null);
            viewHolder = new ViewHolder();
            viewHolder.ivPic = (ImageView) convertView.findViewById(R.id.iv_icon);
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.iv_title);
            viewHolder.tvContent = (TextView) convertView.findViewById(R.id.iv_content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //以上就是控件的实例化，下面就是设置数据
        viewHolder.ivPic.setImageResource(R.mipmap.ic_launcher);
        //1.这里我重新去创建ImageLoader，不用以前我封装好的！使用线程池的方式去加载图片
        if (mIsListViewIdle) {
            new ImageLoader().showImageByThreads(viewHolder.ivPic, mList.get(position).newsPic);
        }

        //2.使用AsyncTask去加载图片
        //new ImageLoader().showImagebyAsyncTask(viewHolder.ivPic, mList.get(position).newsPic);
        viewHolder.tvTitle.setText(mList.get(position).newsTitle);
        viewHolder.tvContent.setText(mList.get(position).newsContent);
        return convertView;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            mIsListViewIdle = true;
            this.notifyDataSetChanged();
        } else {
            mIsListViewIdle = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    class ViewHolder {
        public ImageView ivPic;
        public TextView tvTitle;
        public TextView tvContent;
    }
}
