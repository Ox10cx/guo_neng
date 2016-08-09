package com.watch.guoneng.util;

import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.watch.guoneng.app.MyApplication;
import com.watch.guoneng.tool.Lg;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 访问网络的工具类
 *
 * @author 黄家强
 */
public class HttpUtil {
    /**
     * 测试版本ip
     */
//    public static String IP = "192.168.1.213:4000";

    /**
     * 发布版ip
     */

    //public static String IP = "112.74.23.39:4000";

    /**
     * http版长连接
     */
    public static String IP = "112.74.23.39:4000";

    public static String SERVER = "http://" + IP + "/";
    public static String URL_LOGIN = "http://" + IP + "/server/User/login";
    public static String URL_REGISTER = "http://" + IP + "/server/User/register";
    public static String URL_CHANGPASSWORD = "http://" + IP + "/server/User/changePassword";
    public static String URL_CHECKMOBILE = "http://" + IP + "/server/User/getcheckmobile";
    public static String URL_EDITPROFILE = "http://" + IP + "/server/User/eidtprofile";
    public static String URL_FORGETPASSWORD = "http://" + IP + "/server/User/forgetPassword";
    public static String URL_RESETPASSWORD = "http://" + IP + "/server/User/resetPassword";
    public static String URL_STATICPAGE = "http://" + IP + "/server/User/staticPage";
    public static String URL_UPLOADUSERIMAGE = "http://" + IP + "/server/User/uploadUserImage";
    public static String URL_ANDROIDUPDATE = "http://" + IP + "/ble/updateSoftware";
    public static String URL_LINKWIFIDEVICE = "http://" + IP + "/server/User/linkWifiDevice";
    public static String URL_GETWIFIDEVICE = "http://" + IP + "/server/User/getWifiDevice";
    public static String URL_UNLINKWIFIDEVICE = "http://" + IP + "/server/User/unlinkWifiDevice";
    public static String URL_GETWIFIDEVICELIST = "http://" + IP + "/server/User/getWifiDeviceList";
    public static String URL_UPDATEWIFILOGINSTATUS = "http://" + IP + "/server/User/updateWifiLoginStatus";
    public static String URL_GETLIGHTLIST = "http://" + IP + "/server/User/getLightList";
    public static String URL_ADDLIGHT = "http://" + IP + "/server/User/addLight";
    public static String URL_DELETELight = "http://" + IP + "/server/User/deleteLight";
    public static String URL_UPDATELIGHT = "http://" + IP + "/server/User/updateLight";

    public static String URL_GETMSGID = "http://" + IP + "/server/User/getMessageId";
    public static String URL_SENDMSG = "http://" + IP + "/server/User/sendMessage";
    public static String URL_GETUNREADMSG = "http://" + IP + "/server/User/getUnreadMessage";
    public static String URL_SETREADFLAG = "http://" + IP + "/server/User/setReadFlag";
    public static String URL_EDITDEVICENAME = "http://" + IP + "/server/User/changeWifiName";

    /**
     * 用post方式来访问网络
     *
     * @param url            要访问的网址
     * @param nameValuePairs 需要的参数
     * @return 网络返回的结果数据
     */
    public static String post(String url, NameValuePair... nameValuePairs) {
        HttpClient httpClient = new DefaultHttpClient();
        String msg = "";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (int i = 0; i < nameValuePairs.length; i++) {
            params.add(nameValuePairs[i]);
        }

        String token = MyApplication.getInstance().mToken;
        Log.e("hjq", "token = " + token);
        try {
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            post.addHeader("Authorization", "Bearer " + token);
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
            HttpResponse response = httpClient.execute(post);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                msg = EntityUtils.toString(response.getEntity());
            } else if (status == HttpStatus.SC_UNAUTHORIZED) {
                JSONObject json = new JSONObject();
                json.put("status", "nok");
                json.put("error", "password error");
                json.put("msg", "密码错误");
                msg = json.toString();
            } else {
                Log.e("hjq", "网络请求失败");
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            msg = e.getMessage();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            msg = e.getMessage();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return msg;
    }

    /**
     * 用get方式来访问网络
     *
     * @param url 要访问的网址
     * @return 网络返回的结果数据
     */
    public static String get(String url) {
        HttpClient httpClient = new DefaultHttpClient();
        Lg.i("hjq", "get_url:" + url);
        String msg = "";
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        try {
            response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (entity != null) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(entity.getContent()));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        msg += line;
                    }
                }
            } else {
                Log.e("hjq", "网络请求失败");
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return msg;
    }

    private static String getCookie(Context context) {
        CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie("cookie");
        Log.e("hjq", "getCookie=" + cookie);
        return cookie;
    }

    public static String getURlStr(String url, NameValuePair... namevalues) {
        StringBuilder result = new StringBuilder(url + "?");
        for (int i = 0; i < namevalues.length; i++) {
            if (i != 0) {
                result.append("&" + namevalues[i].getName() + "="
                        + namevalues[i].getValue());
            } else {
                result.append(namevalues[i].getName() + "="
                        + namevalues[i].getValue());
            }

        }
        return result.toString();

    }
}
