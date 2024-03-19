/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.gtask.remote;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.ui.NotesPreferenceActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


/**
 * GTaskClient类用于与Google任务服务进行远程交互。
 * 提供了登录、获取任务列表、添加任务等操作的方法。
 */
public class GTaskClient {
    // 日志标签
    private static final String TAG = GTaskClient.class.getSimpleName();

    // Google任务服务的基础URL
    private static final String GTASK_URL = "https://mail.google.com/tasks/";

    // 用于获取任务信息的URL
    private static final String GTASK_GET_URL = "https://mail.google.com/tasks/ig";

    // 用于提交任务信息的URL
    private static final String GTASK_POST_URL = "https://mail.google.com/tasks/r/ig";

    // 单例模式实例
    private static GTaskClient mInstance = null;

    // HTTP客户端
    private DefaultHttpClient mHttpClient;

    // GET请求URL
    private String mGetUrl;

    // POST请求URL
    private String mPostUrl;

    // 客户端版本号
    private long mClientVersion;

    // 是否已登录
    private boolean mLoggedin;

    // 最后登录时间
    private long mLastLoginTime;

    // 操作ID，用于标识一次操作
    private int mActionId;

    // 用户账户信息
    private Account mAccount;

    // 用于存储更新数据的JSON数组
    private JSONArray mUpdateArray;

    /**
     * GTaskClient的私有构造方法，初始化各种属性。
     */
    private GTaskClient() {
        // 初始化客户端
        mHttpClient = null;
        mGetUrl = GTASK_GET_URL;
        mPostUrl = GTASK_POST_URL;
        mClientVersion = -1;
        mLoggedin = false;
        mLastLoginTime = 0;
        mActionId = 1;
        mAccount = null;
        mUpdateArray = null;
    }

    /**
     * 获取GTaskClient的单例实例。
     *
     * @return GTaskClient的单例实例。
     */
    public static synchronized GTaskClient getInstance() {
        // 确保仅创建一个实例
        if (mInstance == null) {
            mInstance = new GTaskClient();
        }
        return mInstance;
    }

    /**
     * 用户登录函数。
     *
     * @param activity 当前活动，用于获取账户信息和上下文。
     * @return 登录成功返回true，失败返回false。
     */
    public boolean login(Activity activity) {
        // 检查登录是否过期
        final long interval = 1000 * 60 * 5; // 5分钟
        if (mLastLoginTime + interval < System.currentTimeMillis()) {
            mLoggedin = false;
        }

        // 检查账户是否切换，需要重新登录
        if (mLoggedin
                && !TextUtils.equals(getSyncAccount().name, NotesPreferenceActivity
                .getSyncAccountName(activity))) {
            mLoggedin = false;
        }

        // 如果已经登录，则直接返回成功
        if (mLoggedin) {
            Log.d(TAG, "already logged in");
            return true;
        }

        // 记录当前登录时间
        mLastLoginTime = System.currentTimeMillis();
        // 尝试登录Google账户
        String authToken = loginGoogleAccount(activity, false);
        if (authToken == null) {
            Log.e(TAG, "login google account failed");
            return false;
        }

        // 如果是自定义域名邮箱，则尝试使用自定义域名登录
        if (!(mAccount.name.toLowerCase().endsWith("gmail.com") || mAccount.name.toLowerCase()
                .endsWith("googlemail.com"))) {
            // 构造自定义域名的登录URL
            StringBuilder url = new StringBuilder(GTASK_URL).append("a/");
            int index = mAccount.name.indexOf('@') + 1;
            String suffix = mAccount.name.substring(index);
            url.append(suffix + "/");
            mGetUrl = url.toString() + "ig";
            mPostUrl = url.toString() + "r/ig";

            // 尝试使用自定义域名登录
            if (tryToLoginGtask(activity, authToken)) {
                mLoggedin = true;
            }
        }

        // 如果使用自定义域名登录失败，则尝试使用官方URL登录
        if (!mLoggedin) {
            mGetUrl = GTASK_GET_URL;
            mPostUrl = GTASK_POST_URL;
            if (!tryToLoginGtask(activity, authToken)) {
                return false;
            }
        }

        // 登录成功
        mLoggedin = true;
        return true;
    }


    /**
     * 使用Google账户登录，获取授权令牌。
     *
     * @param activity        当前活动，用于获取账户管理器。
     * @param invalidateToken 是否吊销之前的令牌并重新获取。
     * @return 返回获取到的授权令牌，如果失败或没有可用账户返回null。
     */
    private String loginGoogleAccount(Activity activity, boolean invalidateToken) {
        String authToken;
        // 获取账户管理器和所有Google账户
        AccountManager accountManager = AccountManager.get(activity);
        Account[] accounts = accountManager.getAccountsByType("com.google");

        // 检查是否有可用的Google账户
        if (accounts.length == 0) {
            Log.e(TAG, "there is no available google account");
            return null;
        }

        // 根据设置中的账户名选择账户
        String accountName = NotesPreferenceActivity.getSyncAccountName(activity);
        Account account = null;
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                account = a;
                break;
            }
        }
        // 检查是否找到设置中对应的账户
        if (account != null) {
            mAccount = account;
        } else {
            Log.e(TAG, "unable to get an account with the same name in the settings");
            return null;
        }

        // 获取授权令牌
        AccountManagerFuture<Bundle> accountManagerFuture = accountManager.getAuthToken(account,
                "goanna_mobile", null, activity, null, null);
        try {
            Bundle authTokenBundle = accountManagerFuture.getResult();
            authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            // 如果需要，吊销令牌并重新获取
            if (invalidateToken) {
                accountManager.invalidateAuthToken("com.google", authToken);
                loginGoogleAccount(activity, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "get auth token failed");
            authToken = null;
        }

        return authToken;
    }

    /**
     * 尝试使用授权令牌登录Gtask。
     *
     * @param activity  当前活动，用于登录过程中的UI交互。
     * @param authToken 授权令牌。
     * @return 如果登录成功返回true，否则返回false。
     */
    private boolean tryToLoginGtask(Activity activity, String authToken) {
        // 首次尝试登录Gtask
        if (!loginGtask(authToken)) {
            // 如果失败，尝试吊销令牌并重新获取后再次登录
            authToken = loginGoogleAccount(activity, true);
            if (authToken == null) {
                Log.e(TAG, "login google account failed");
                return false;
            }

            // 使用新令牌再次尝试登录Gtask
            if (!loginGtask(authToken)) {
                Log.e(TAG, "login gtask failed");
                return false;
            }
        }
        return true;
    }

    /**
     * 执行Gtask登录操作。
     *
     * @param authToken 授权令牌。
     * @return 登录成功返回true，失败返回false。
     */
    private boolean loginGtask(String authToken) {
        // 设置HTTP连接参数
        int timeoutConnection = 10000;
        int timeoutSocket = 15000;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        mHttpClient = new DefaultHttpClient(httpParameters);
        BasicCookieStore localBasicCookieStore = new BasicCookieStore();
        mHttpClient.setCookieStore(localBasicCookieStore);
        HttpProtocolParams.setUseExpectContinue(mHttpClient.getParams(), false);

        // 使用授权令牌登录Gtask
        try {
            String loginUrl = mGetUrl + "?auth=" + authToken;
            HttpGet httpGet = new HttpGet(loginUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // 检查是否获取到授权Cookie
            List<Cookie> cookies = mHttpClient.getCookieStore().getCookies();
            boolean hasAuthCookie = false;
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("GTL")) {
                    hasAuthCookie = true;
                }
            }
            if (!hasAuthCookie) {
                Log.w(TAG, "it seems that there is no auth cookie");
            }

            // 解析响应，获取客户端版本
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            mClientVersion = js.getLong("v");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "httpget gtask_url failed");
            return false;
        }

        return true;
    }


    /**
     * 获取一个唯一的动作ID
     *
     * @return 返回当前动作的ID，每次调用自增
     */
    private int getActionId() {
        return mActionId++;
    }

    /**
     * 创建一个HttpPost请求
     *
     * @return 配置好的HttpPost对象
     */
    private HttpPost createHttpPost() {
        HttpPost httpPost = new HttpPost(mPostUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        httpPost.setHeader("AT", "1");
        return httpPost;
    }

    /**
     * 从HttpEntity中获取响应内容
     *
     * @param entity Http响应实体
     * @return 响应内容的字符串
     * @throws IOException 当读取响应内容失败时抛出
     */
    private String getResponseContent(HttpEntity entity) throws IOException {
        String contentEncoding = null;
        if (entity.getContentEncoding() != null) {
            contentEncoding = entity.getContentEncoding().getValue();
            Log.d(TAG, "encoding: " + contentEncoding);
        }

        InputStream input = entity.getContent();
        // 根据内容编码类型，对输入流进行解压
        if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
            input = new GZIPInputStream(entity.getContent());
        } else if (contentEncoding != null && contentEncoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true);
            input = new InflaterInputStream(entity.getContent(), inflater);
        }

        try {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();

            // 读取并构建响应内容字符串
            while (true) {
                String buff = br.readLine();
                if (buff == null) {
                    return sb.toString();
                }
                sb = sb.append(buff);
            }
        } finally {
            input.close();
        }
    }

    /**
     * 发送POST请求，并返回解析后的JSONObject
     *
     * @param js 要发送的JSON对象
     * @return 请求响应的JSONObject
     * @throws NetworkFailureException 当网络请求或处理失败时抛出
     */
    private JSONObject postRequest(JSONObject js) throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        HttpPost httpPost = createHttpPost();
        try {
            LinkedList<BasicNameValuePair> list = new LinkedList<BasicNameValuePair>();
            list.add(new BasicNameValuePair("r", js.toString()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
            httpPost.setEntity(entity);

            // 执行POST请求
            HttpResponse response = mHttpClient.execute(httpPost);
            String jsString = getResponseContent(response.getEntity());
            return new JSONObject(jsString);

        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("postRequest failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("unable to convert response content to jsonobject");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("error occurs when posting request");
        }
    }

    /**
     * 创建一个任务
     *
     * @param task 要创建的任务对象
     * @throws NetworkFailureException 当网络操作失败时抛出
     */
    public void createTask(Task task) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();

            // 构建动作列表
            actionList.put(task.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本信息
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送请求并处理响应
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            task.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create task: handling jsonobject failed");
        }
    }


    /**
     * 创建任务列表。
     *
     * @param tasklist 任务列表对象，包含创建任务所需的信息。
     * @throws NetworkFailureException 网络请求失败时抛出。
     */
    public void createTaskList(TaskList tasklist) throws NetworkFailureException {
        commitUpdate(); // 提交更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建POST请求的JSON对象
            JSONArray actionList = new JSONArray(); // 动作列表

            // 添加创建任务的动作到动作列表
            actionList.put(tasklist.getCreateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本信息
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            // 发送POST请求并处理响应
            JSONObject jsResponse = postRequest(jsPost);
            JSONObject jsResult = (JSONObject) jsResponse.getJSONArray(
                    GTaskStringUtils.GTASK_JSON_RESULTS).get(0);
            tasklist.setGid(jsResult.getString(GTaskStringUtils.GTASK_JSON_NEW_ID));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("create tasklist: handing jsonobject failed");
        }
    }

    /**
     * 提交待更新的任务信息。
     *
     * @throws NetworkFailureException 网络请求失败时抛出。
     */
    public void commitUpdate() throws NetworkFailureException {
        if (mUpdateArray != null) {
            try {
                JSONObject jsPost = new JSONObject(); // 创建POST请求的JSON对象

                // 添加更新的动作列表
                jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, mUpdateArray);

                // 添加客户端版本信息
                jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

                postRequest(jsPost); // 发送POST请求
                mUpdateArray = null; // 清空更新数组

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("commit update: handing jsonobject failed");
            }
        }
    }

    /**
     * 添加一个待更新的任务节点。
     *
     * @param node 待添加的节点信息。
     * @throws NetworkFailureException 网络请求失败时抛出。
     */
    public void addUpdateNode(Node node) throws NetworkFailureException {
        if (node != null) {
            // 若更新节点过多，则提交当前更新
            if (mUpdateArray != null && mUpdateArray.length() > 10) {
                commitUpdate();
            }

            if (mUpdateArray == null)
                mUpdateArray = new JSONArray(); // 创建更新节点的数组
            mUpdateArray.put(node.getUpdateAction(getActionId())); // 添加节点更新动作
        }
    }

    /**
     * 移动任务到不同的任务列表或在同一任务列表内移动位置。
     *
     * @param task      要移动的任务。
     * @param preParent 任务的原父任务列表。
     * @param curParent 任务的新父任务列表。
     * @throws NetworkFailureException 网络请求失败时抛出。
     */
    public void moveTask(Task task, TaskList preParent, TaskList curParent)
            throws NetworkFailureException {
        commitUpdate(); // 提交当前更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建POST请求的JSON对象
            JSONArray actionList = new JSONArray(); // 动作列表
            JSONObject action = new JSONObject(); // 单个动作

            // 添加移动任务的动作
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_MOVE);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_ID, task.getGid());
            if (preParent == curParent && task.getPriorSibling() != null) {
                // 如果在同一任务列表内移动且不是第一个任务，则添加前置兄弟节点ID
                action.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, task.getPriorSibling());
            }
            action.put(GTaskStringUtils.GTASK_JSON_SOURCE_LIST, preParent.getGid());
            action.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT, curParent.getGid());
            if (preParent != curParent) {
                // 如果跨任务列表移动，添加目标任务列表ID
                action.put(GTaskStringUtils.GTASK_JSON_DEST_LIST, curParent.getGid());
            }
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本信息
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost); // 发送POST请求

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("move task: handing jsonobject failed");
        }
    }

    /**
     * 删除指定的任务节点。
     *
     * @param node 要删除的节点。
     * @throws NetworkFailureException 网络请求失败时抛出。
     */
    public void deleteNode(Node node) throws NetworkFailureException {
        commitUpdate(); // 提交当前更新

        try {
            JSONObject jsPost = new JSONObject(); // 创建POST请求的JSON对象
            JSONArray actionList = new JSONArray(); // 动作列表

            // 添加删除节点的动作
            node.setDeleted(true);
            actionList.put(node.getUpdateAction(getActionId()));
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 添加客户端版本信息
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);

            postRequest(jsPost); // 发送POST请求
            mUpdateArray = null; // 清空更新数组

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("delete node: handing jsonobject failed");
        }
    }


    /**
     * 获取任务列表的网络请求。
     * 注意：调用此方法前需要确保用户已经登录。
     *
     * @return JSONArray 返回一个包含任务列表的JSON数组。
     * @throws NetworkFailureException 如果网络请求失败则抛出此异常。
     */
    public JSONArray getTaskLists() throws NetworkFailureException {
        if (!mLoggedin) {
            Log.e(TAG, "please login first");
            throw new ActionFailureException("not logged in");
        }

        try {
            HttpGet httpGet = new HttpGet(mGetUrl);
            HttpResponse response = null;
            response = mHttpClient.execute(httpGet);

            // 从响应中提取任务列表
            String resString = getResponseContent(response.getEntity());
            String jsBegin = "_setup(";
            String jsEnd = ")}</script>";
            int begin = resString.indexOf(jsBegin);
            int end = resString.lastIndexOf(jsEnd);
            String jsString = null;
            if (begin != -1 && end != -1 && begin < end) {
                jsString = resString.substring(begin + jsBegin.length(), end);
            }
            JSONObject js = new JSONObject(jsString);
            return js.getJSONObject("t").getJSONArray(GTaskStringUtils.GTASK_JSON_LISTS);
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new NetworkFailureException("gettasklists: httpget failed");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task lists: handling json object failed");
        }
    }

    /**
     * 根据列表ID获取特定任务列表的网络请求。
     *
     * @param listGid 列表的全局唯一标识符。
     * @return JSONArray 返回一个包含特定任务列表的JSON数组。
     * @throws NetworkFailureException 如果网络请求失败则抛出此异常。
     */
    public JSONArray getTaskList(String listGid) throws NetworkFailureException {
        commitUpdate();
        try {
            JSONObject jsPost = new JSONObject();
            JSONArray actionList = new JSONArray();
            JSONObject action = new JSONObject();

            // 构建请求参数
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_GETALL);
            action.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, getActionId());
            action.put(GTaskStringUtils.GTASK_JSON_LIST_ID, listGid);
            action.put(GTaskStringUtils.GTASK_JSON_GET_DELETED, false);
            actionList.put(action);
            jsPost.put(GTaskStringUtils.GTASK_JSON_ACTION_LIST, actionList);

            // 发送请求并处理响应
            jsPost.put(GTaskStringUtils.GTASK_JSON_CLIENT_VERSION, mClientVersion);
            JSONObject jsResponse = postRequest(jsPost);
            return jsResponse.getJSONArray(GTaskStringUtils.GTASK_JSON_TASKS);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("get task list: handling json object failed");
        }
    }

    /**
     * 获取同步账户信息。
     *
     * @return Account 返回当前的同步账户。
     */
    public Account getSyncAccount() {
        return mAccount;
    }

    /**
     * 重置更新数组。
     * 用于在进行新的同步之前清空或重置更新的数据数组。
     */
    public void resetUpdateArray() {
        mUpdateArray = null;
    }

}
