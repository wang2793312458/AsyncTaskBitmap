package com.geyan.testing.asynctaskdemo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private static final String URL = "http://www.imooc.com/api/teacher?type=4&num=30";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.lv_listItem);
        new NewsAsyncTask().execute(URL);
    }


    /**
     * 分析3个参数传入的到底是什么，最后一个得到的Result是新闻Item的List集合，而不是单单NewsBean对象
     */
    class NewsAsyncTask extends AsyncTask<String, Void, List<NewsBean>> {

        /**
         * 在子线程中执行，返回Json数据
         *
         * @param params
         * @return
         */
        @Override
        protected List<NewsBean> doInBackground(String... params) {

            return getJsonData(params[0]);
        }

        @Override
        protected void onPostExecute(List<NewsBean> data) {
            mListView.setAdapter(new NewsAdapter(MainActivity.this, data,mListView));
        }
    }

    //获取Json数据，添加到List集合中
    private List<NewsBean> getJsonData(String urlString) {

        List<NewsBean> newsBeanList = new ArrayList<>();
        //获取网页返回的字符串
        HttpURLConnection urlConnection = null;
        BufferedReader bufr = null;
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream is = urlConnection.getInputStream();
            bufr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = "";
            while ((line = bufr.readLine()) != null) {
                sb.append(line);
            }
            Log.d("gy", "JsonData" + sb.toString());

            //解析Json数据
            JSONObject jsonObject = null;
            NewsBean newsBean = null;
            try {
                jsonObject = new JSONObject(sb.toString());
                JSONArray jsonArray = jsonObject.getJSONArray("data");
                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    newsBean = new NewsBean();
                    newsBean.newsPic = jsonObject.getString("picSmall");
                    newsBean.newsTitle = jsonObject.getString("name");
                    newsBean.newsContent = jsonObject.getString("description");
                    newsBeanList.add(newsBean);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newsBeanList;
    }
}
