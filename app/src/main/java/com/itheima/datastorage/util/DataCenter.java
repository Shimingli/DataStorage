package com.itheima.datastorage.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

/**
 * 数据中心：处理数据存储，获取数据
 * youliang.ji on 2016/5/1.
 */
public class DataCenter {

    private static DataCenter instance;
    private static HotelSQLiteOpenHelper openHelper;
    private final static String url = "http://192.168.0.107:8080/VolleyTest/HotelList";
    private static Context context;
    /***返回码：网络数据*/
    public final static int RESP_CODE_INTERNET = 240;
    /**返回码：缓冲数据*/
    public final static int RESP_CODE_CACHE = 244;

    public static DataCenter getInstance(Context context) {
        if (null == instance) {
            synchronized (DataCenter.class) {
                if (null == instance) {
                    instance = new DataCenter(context);
                    DataCenter.context = context;
                }
            }
        }
        return instance;
    }

    private DataCenter(Context context) {
        openHelper = new HotelSQLiteOpenHelper(context);
    }


    /**
     * json字符串存储到数据库中
     *
     * @param pagenum 页码
     * @param data
     */
    public void saveJsonString(final int pagenum, final String data) {
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        LogUtil.d("将第" + pagenum + "页数据存储到数据库");
        new Thread(new Runnable() {
            @Override
            public void run() {//避免数据过大耗时，放到子线程中
                ContentValues values = new ContentValues();//Map，包含要插入的数据
                values.put("pagenum", pagenum);
                values.put("data", data);
                long count = db.insert("hotel_list", null, values);
                db.close();

            }
        }).start();

    }

    /**
     * 从数据库获取数据
     *
     * @param pagenum
     */
    private String getHotelListFromDatabase(int pagenum) {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        String hotelList = null;
        if (db != null) {
            Cursor cursor = db.query("hotel_list", new String[]{"data"}, "pagenum=?", new String[]{pagenum + ""}, null, null, null);
            while (cursor.moveToNext()) {
                hotelList = cursor.getString(0);
            }
            db.close();
        }
        return hotelList;
    }

    /**
     * 删除所有酒店列表数据
     */
    public void deleteAll() {
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        new Thread(new Runnable() {
            @Override
            public void run() {//子线程删除数据
                int count = db.delete("hotel_list", null, null);
                db.close();
                LogUtil.d("删除数据条数：" + count);
            }
        }).start();

    }

    /**
     * 获取酒店列表数据：从网络或者从缓存中获取
     *  @param pagenum  页码
     * @param queue
     * @param listener 回调接口
     */
    public void getHotelList(String reqFlag, int pagenum, RequestQueue queue, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        //1.从缓冲中读取，如果存在，取出来
        String data = getHotelListFromDatabase(pagenum);
        if(TextUtils.isEmpty(data)){
            //2.如果不存在，从网络获取
            getHotelListDataFromInternet(reqFlag, pagenum, queue,listener , errorListener);
        }else{
            LogUtil.d("从缓冲获取第"+pagenum+"页数据");
            listener.onResponse(data);
        }
    }


    private void getHotelListDataFromInternet(String reqFlag, final int pagenum, RequestQueue queue, final Response.Listener listener, Response.ErrorListener errorListener) {
        LogUtil.d("从网络获取第" + pagenum + "页数据");
        //下面之所以new Listener，而不直接返回数据，是为了将缓冲数据逻辑和UI解绑，这样不用每个Activity页面都处理缓冲数据
        StringRequest request = new StringRequest(url + "?pagenum=" + pagenum, new Response.Listener<String>() {
            @Override
            public void onResponse(String data) {
                listener.onResponse(data);
                saveJsonString(pagenum, data);//保存到数据库
            }
        }, errorListener);

        request.setTag(reqFlag);
        queue.add(request);//发起网络请求

    }
}