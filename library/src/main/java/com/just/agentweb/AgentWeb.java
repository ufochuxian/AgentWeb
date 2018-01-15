package com.just.agentweb;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * https://github.com/Justson/AgentWeb
 * author just -- cxz
 */
public final class AgentWeb {

    private static final String TAG = AgentWeb.class.getSimpleName();
    private Activity mActivity;
    private ViewGroup mViewGroup;
    private WebCreator mWebCreator;
    private AgentWebSettings mAgentWebSettings;
    private AgentWeb mAgentWeb = null;
    private IndicatorController mIndicatorController;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;
    private boolean enableProgress;
    private Fragment mFragment;
    private IEventHandler mIEventHandler;
    private ArrayMap<String, Object> mJavaObjects = new ArrayMap<>();
    private int TAG_TARGET = 0;
    private WebListenerManager mWebListenerManager;
    private DownloadListener mDownloadListener = null;
    private ChromeClientCallbackManager mChromeClientCallbackManager;
    private WebSecurityController<WebSecurityCheckLogic> mWebSecurityController = null;
    private WebSecurityCheckLogic mWebSecurityCheckLogic = null;
    private WebChromeClient mTargetChromeClient;
    private SecurityType mSecurityType = SecurityType.DEFAULT_CHECK;
    private static final int ACTIVITY_TAG = 0;
    private static final int FRAGMENT_TAG = 1;
    private AgentWebJsInterfaceCompat mAgentWebJsInterfaceCompat = null;
    private JsEntraceAccess mJsEntraceAccess = null;
    private ILoader mILoader = null;
    private WebLifeCycle mWebLifeCycle;
    private IVideo mIVideo = null;
    private boolean webClientHelper = true;
    private DefaultMsgConfig mDefaultMsgConfig;
    private PermissionInterceptor mPermissionInterceptor;
    private boolean isInterceptUnkownScheme = false;
    private int openOtherAppWays = -1;
    private MiddleWareWebClientBase mMiddleWrareWebClientBaseHeader;
    private MiddleWareWebChromeBase mMiddleWareWebChromeBaseHeader;
    private EventInterceptor mEventInterceptor;


    private AgentWeb(AgentBuilder agentBuilder) {
        TAG_TARGET = FRAGMENT_TAG;
        this.mActivity = agentBuilder.mActivity;
        this.mFragment = agentBuilder.mFragment;
        this.mViewGroup = agentBuilder.mViewGroup;
        this.mIEventHandler = agentBuilder.mIEventHandler;
        this.enableProgress = agentBuilder.enableProgress;
        mWebCreator = agentBuilder.mWebCreator == null ? configWebCreator(agentBuilder.v, agentBuilder.index, agentBuilder.mLayoutParams, agentBuilder.mIndicatorColor, agentBuilder.height_dp, agentBuilder.mWebView, agentBuilder.mWebLayout) : agentBuilder.mWebCreator;
        mIndicatorController = agentBuilder.mIndicatorController;
        this.mWebChromeClient = agentBuilder.mWebChromeClient;
        this.mWebViewClient = agentBuilder.mWebViewClient;
        mAgentWeb = this;
        this.mAgentWebSettings = agentBuilder.mAgentWebSettings;

        if (agentBuilder.mJavaObject != null && !agentBuilder.mJavaObject.isEmpty()) {
            this.mJavaObjects.putAll((Map<? extends String, ?>) agentBuilder.mJavaObject);
            LogUtils.i(TAG, "mJavaObject size:" + this.mJavaObjects.size());

        }
        this.mChromeClientCallbackManager = agentBuilder.mChromeClientCallbackManager;
        this.mPermissionInterceptor = agentBuilder.mPermissionInterceptor == null ? null : new PermissionInterceptorWrapper(agentBuilder.mPermissionInterceptor);
        this.mWebViewClientCallbackManager = agentBuilder.mWebViewClientCallbackManager;
        this.mSecurityType = agentBuilder.mSecurityType;
        this.mILoader = new LoaderImpl(mWebCreator.create().get(), agentBuilder.mHttpHeaders);
        if (this.mWebCreator.getGroup() instanceof WebParentLayout) {
            WebParentLayout mWebParentLayout = (WebParentLayout) this.mWebCreator.getGroup();
            mWebParentLayout.bindController(agentBuilder.mAgentWebUIController == null ? AgentWebUIControllerImplBase.build() : agentBuilder.mAgentWebUIController);

            mWebParentLayout.setErrorLayoutRes(agentBuilder.errorLayout, agentBuilder.reloadId);
            mWebParentLayout.setErrorView(agentBuilder.errorView);
        }
        this.mWebLifeCycle = new DefaultWebLifeCycleImpl(mWebCreator.get());
        mWebSecurityController = new WebSecurityControllerImpl(mWebCreator.get(), this.mAgentWeb.mJavaObjects, this.mSecurityType);
        this.webClientHelper = agentBuilder.webClientHelper;
        this.isInterceptUnkownScheme = agentBuilder.isInterceptUnkownScheme;
        if (agentBuilder.openOtherPage != null) {
            this.openOtherAppWays = agentBuilder.openOtherPage.code;
        }
        this.mMiddleWrareWebClientBaseHeader = agentBuilder.header;
        this.mMiddleWareWebChromeBaseHeader = agentBuilder.mChromeMiddleWareHeader;
        init();
        setDownloadListener(agentBuilder.mDownLoadResultListeners, agentBuilder.isParallelDownload, agentBuilder.icon);
    }

    private void init() {
        if (this.mDownloadListener == null)
            mDefaultMsgConfig = new DefaultMsgConfig();
        doCompat();
        doSafeCheck();
    }

    public DefaultMsgConfig getDefaultMsgConfig() {
        return this.mDefaultMsgConfig;
    }


    public PermissionInterceptor getPermissionInterceptor() {
        return this.mPermissionInterceptor;
    }

    private void doCompat() {


        mJavaObjects.put("agentWeb", mAgentWebJsInterfaceCompat = new AgentWebJsInterfaceCompat(this, mActivity));

        LogUtils.i(TAG, "AgentWebConfig.isUseAgentWebView:" + AgentWebConfig.WEBVIEW_TYPE + "  mChromeClientCallbackManager:" + mChromeClientCallbackManager);
        if (AgentWebConfig.WEBVIEW_TYPE == AgentWebConfig.WEBVIEW_AGENTWEB_SAFE_TYPE) {
            this.mChromeClientCallbackManager.setAgentWebCompatInterface((ChromeClientCallbackManager.AgentWebCompatInterface) mWebCreator.get());
            this.mWebViewClientCallbackManager.setPageLifeCycleCallback((WebViewClientCallbackManager.PageLifeCycleCallback) mWebCreator.get());
        }

    }

    public WebLifeCycle getWebLifeCycle() {
        return this.mWebLifeCycle;
    }

    private void doSafeCheck() {

        WebSecurityCheckLogic mWebSecurityCheckLogic = this.mWebSecurityCheckLogic;
        if (mWebSecurityCheckLogic == null) {
            this.mWebSecurityCheckLogic = mWebSecurityCheckLogic = WebSecurityLogicImpl.getInstance();
        }
        mWebSecurityController.check(mWebSecurityCheckLogic);

    }

    private WebCreator configWebCreator(BaseIndicatorView progressView, int index, ViewGroup.LayoutParams lp, int mIndicatorColor, int height_dp, WebView webView, IWebLayout webLayout) {

        if (progressView != null && enableProgress) {
            return new DefaultWebCreator(mActivity, mViewGroup, lp, index, progressView, webView, webLayout);
        } else {
            return enableProgress ?
                    new DefaultWebCreator(mActivity, mViewGroup, lp, index, mIndicatorColor, height_dp, webView, webLayout)
                    : new DefaultWebCreator(mActivity, mViewGroup, lp, index, webView, webLayout);
        }
    }

    private void loadData(String data, String mimeType, String encoding) {
        mWebCreator.get().loadData(data, mimeType, encoding);
    }

    private void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String history) {
        mWebCreator.get().loadDataWithBaseURL(baseUrl, data, mimeType, encoding, history);
    }


    public JsEntraceAccess getJsEntraceAccess() {

        JsEntraceAccess mJsEntraceAccess = this.mJsEntraceAccess;
        if (mJsEntraceAccess == null) {
            this.mJsEntraceAccess = mJsEntraceAccess = JsEntraceAccessImpl.getInstance(mWebCreator.get());
        }
        return mJsEntraceAccess;
    }


    public AgentWeb clearWebCache() {

        if (this.getWebCreator().get() != null) {
            Log.i(TAG, "清空 webview 缓存");
            AgentWebUtils.clearWebViewAllCache(mActivity, this.getWebCreator().get());
        } else {
            AgentWebUtils.clearWebViewAllCache(mActivity);
        }
        return this;
    }


    public static AgentBuilder with(@NonNull Activity activity) {
        if (activity == null)
            throw new NullPointerException("activity can not be null .");
        return new AgentBuilder(activity);
    }

    public static AgentBuilder with(@NonNull Fragment fragment) {


        Activity mActivity = null;
        if ((mActivity = fragment.getActivity()) == null)
            throw new NullPointerException("activity can not be null .");
        return new AgentBuilder(mActivity, fragment);
    }


    private EventInterceptor getInterceptor() {

        if (this.mEventInterceptor != null)
            return this.mEventInterceptor;

        if (mIVideo instanceof VideoImpl) {
            return this.mEventInterceptor = (EventInterceptor) this.mIVideo;
        }

        return null;

    }

    public boolean handleKeyEvent(int keyCode, KeyEvent keyEvent) {

        if (mIEventHandler == null) {
            mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor());
        }
        return mIEventHandler.onKeyDown(keyCode, keyEvent);
    }

    public boolean back() {

        if (mIEventHandler == null) {
            mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor());
        }
        return mIEventHandler.back();
    }


    public WebCreator getWebCreator() {
        return this.mWebCreator;
    }

    public IEventHandler getIEventHandler() {
        return this.mIEventHandler == null ? (this.mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor())) : this.mIEventHandler;
    }

    private JsInterfaceHolder mJsInterfaceHolder = null;

    public AgentWebSettings getAgentWebSettings() {
        return this.mAgentWebSettings;
    }

    public IndicatorController getIndicatorController() {
        return this.mIndicatorController;
    }


    private AgentWeb ready() {

        AgentWebConfig.initCookiesManager(mActivity.getApplicationContext());
        AgentWebSettings mAgentWebSettings = this.mAgentWebSettings;
        if (mAgentWebSettings == null) {
            this.mAgentWebSettings = mAgentWebSettings = WebDefaultSettingsManager.getInstance();
        }
        if (mWebListenerManager == null && mAgentWebSettings instanceof WebDefaultSettingsManager) {
            mWebListenerManager = (WebListenerManager) mAgentWebSettings;
        }
        mAgentWebSettings.toSetting(mWebCreator.get());
        if (mJsInterfaceHolder == null) {
            mJsInterfaceHolder = JsInterfaceHolderImpl.getJsInterfaceHolder(mWebCreator.get(), this.mSecurityType);
        }
        LogUtils.i(TAG, "mJavaObjects:" + mJavaObjects.size());
        if (mJavaObjects != null && !mJavaObjects.isEmpty()) {
            mJsInterfaceHolder.addJavaObjects(mJavaObjects);
        }

        if (mWebListenerManager != null) {
            mWebListenerManager.setDownLoader(mWebCreator.get(), getLoadListener());
            mWebListenerManager.setWebChromeClient(mWebCreator.get(), getChromeClient());
            mWebListenerManager.setWebViewClient(mWebCreator.get(), getWebViewClient());
        }

        return this;
    }


    private void setDownloadListener(List<DownLoadResultListener> downLoadResultListeners, boolean isParallelDl, int icon) {
        DownloadListener mDownloadListener = this.mDownloadListener;
        if (mDownloadListener == null) {
            this.mDownloadListener = mDownloadListener = new DefaultDownLoaderImpl.Builder().setActivity(mActivity)
                    .setEnableIndicator(true)//
                    .setForce(false)//
                    .setDownLoadResultListeners(downLoadResultListeners)//
                    .setDownLoadMsgConfig(mDefaultMsgConfig.getDownLoadMsgConfig())//
                    .setParallelDownload(isParallelDl)//
                    .setPermissionInterceptor(this.mPermissionInterceptor)
                    .setIcon(icon)
                    .setWebView(this.mWebCreator.get())
                    .create();

        }
    }

    private DownloadListener getLoadListener() {
        DownloadListener mDownloadListener = this.mDownloadListener;
        return mDownloadListener;
    }

    private WebViewClientCallbackManager mWebViewClientCallbackManager = null;

    private WebChromeClient getChromeClient() {
        IndicatorController mIndicatorController = (this.mIndicatorController == null) ? IndicatorHandler.getInstance().inJectProgressView(mWebCreator.offer()) : this.mIndicatorController;

        DefaultChromeClient mDefaultChromeClient =
                new DefaultChromeClient(this.mActivity, this.mIndicatorController = mIndicatorController, this.mWebChromeClient, this.mChromeClientCallbackManager, this.mIVideo = getIVideo(), mDefaultMsgConfig.getChromeClientMsgCfg(), this.mPermissionInterceptor, mWebCreator.get());

        LogUtils.i(TAG, "WebChromeClient:" + this.mWebChromeClient);
        MiddleWareWebChromeBase header = this.mMiddleWareWebChromeBaseHeader;
        if (header != null) {
            MiddleWareWebChromeBase tail = header;
            int count = 1;
            MiddleWareWebChromeBase tmp = header;
            while (tmp.next() != null) {
                tail = tmp = tmp.next();
                count++;
            }
            LogUtils.i(TAG, "MiddleWareWebClientBase middleware count:" + count);
            tail.setWebChromeClient(mDefaultChromeClient);
            return this.mTargetChromeClient = header;
        } else {
            return this.mTargetChromeClient = mDefaultChromeClient;
        }
    }

    private IVideo getIVideo() {
        return mIVideo == null ? new VideoImpl(mActivity, mWebCreator.get()) : mIVideo;
    }

    private WebViewClient getWebViewClient() {

        LogUtils.i(TAG, "getWebViewClient:" + this.mMiddleWrareWebClientBaseHeader);
        DefaultWebClient mDefaultWebClient = DefaultWebClient
                .createBuilder()
                .setActivity(this.mActivity)
                .setClient(this.mWebViewClient)
                .setManager(this.mWebViewClientCallbackManager)
                .setWebClientHelper(this.webClientHelper)
                .setPermissionInterceptor(this.mPermissionInterceptor)
                .setWebView(this.mWebCreator.get())
                .setInterceptUnkownScheme(this.isInterceptUnkownScheme)
                .setSchemeHandleType(this.openOtherAppWays)
                .setCfg(this.mDefaultMsgConfig.getWebViewClientMsgCfg())
                .build();
        MiddleWareWebClientBase header = this.mMiddleWrareWebClientBaseHeader;
        if (header != null) {
            MiddleWareWebClientBase tail = header;
            int count = 1;
            MiddleWareWebClientBase tmp = header;
            while (tmp.next() != null) {
                tail = tmp = tmp.next();
                count++;
            }
            LogUtils.i(TAG, "MiddleWareWebClientBase middleware count:" + count);
            tail.setWebViewClient(mDefaultWebClient);
            return header;
        } else {
            return mDefaultWebClient;
        }

    }

    public JsInterfaceHolder getJsInterfaceHolder() {
        return this.mJsInterfaceHolder;
    }

    @Deprecated
    private WebViewClient getClient() {

        if (!webClientHelper && AgentWebConfig.WEBVIEW_TYPE != AgentWebConfig.WEBVIEW_AGENTWEB_SAFE_TYPE && mWebViewClient != null) {
            return mWebViewClient;
        } else {
            LogUtils.i(TAG, "isInterceptUnkownScheme:" + isInterceptUnkownScheme + "   openOtherAppWays:" + openOtherAppWays);
            return DefaultWebClient
                    .createBuilder()
                    .setActivity(this.mActivity)
                    .setClient(this.mWebViewClient)
                    .setManager(this.mWebViewClientCallbackManager)
                    .setWebClientHelper(this.webClientHelper)
                    .setPermissionInterceptor(this.mPermissionInterceptor)
                    .setWebView(this.mWebCreator.get())
                    .setInterceptUnkownScheme(this.isInterceptUnkownScheme)
                    .setSchemeHandleType(this.openOtherAppWays)
                    .setCfg(this.mDefaultMsgConfig.getWebViewClientMsgCfg())
                    .build();
        }


    }


    public ILoader getLoader() {
        return this.mILoader;
    }


    private AgentWeb go(String url) {
        this.getLoader().loadUrl(url);
        IndicatorController mIndicatorController = null;
        if (!TextUtils.isEmpty(url) && (mIndicatorController = getIndicatorController()) != null && mIndicatorController.offerIndicator() != null) {
            getIndicatorController().offerIndicator().show();
        }
        return this;
    }


    public void destroy() {
        this.mWebLifeCycle.onDestroy();
    }

    public void destroyAndKill() {
        destroy();
        if (!AgentWebUtils.isMainProcess(mActivity)) {
            LogUtils.i(TAG, "退出进程");
            System.exit(0);
        }
    }

    @Deprecated
    public void uploadFileResult(int requestCode, int resultCode, Intent data) {

        IFileUploadChooser mIFileUploadChooser = null;

        if (mTargetChromeClient instanceof DefaultChromeClient) {
            DefaultChromeClient mDefaultChromeClient = (DefaultChromeClient) mTargetChromeClient;
            mIFileUploadChooser = mDefaultChromeClient.pop();
        }

        if (mIFileUploadChooser == null)
            mIFileUploadChooser = mAgentWebJsInterfaceCompat.pop();
        LogUtils.i(TAG, "file upload:" + mIFileUploadChooser);
        if (mIFileUploadChooser != null)
            mIFileUploadChooser.fetchFilePathFromIntent(requestCode, resultCode, data);

        if (mIFileUploadChooser != null)
            mIFileUploadChooser = null;
    }


    public static class PreAgentWeb {
        private AgentWeb mAgentWeb;
        private boolean isReady = false;

        PreAgentWeb(AgentWeb agentWeb) {
            this.mAgentWeb = agentWeb;
        }


        public PreAgentWeb ready() {
            if (!isReady) {
                mAgentWeb.ready();
                isReady = true;
            }
            return this;
        }

        public AgentWeb go(@Nullable String url) {
            if (!isReady) {
//                throw new IllegalStateException(" please call ready before go to finish all webview settings");  //i want to do this , but i cannot;
                ready();
            }
            return mAgentWeb.go(url);
        }


    }


    public enum SecurityType {
        DEFAULT_CHECK, STRICT_CHECK;
    }


    public static final class AgentBuilder {
        private Activity mActivity;
        private Fragment mFragment;

        private ViewGroup mViewGroup;
        private boolean isNeedDefaultProgress;
        private int index = -1;
        private BaseIndicatorView v;
        private IndicatorController mIndicatorController = null;
        /*默认进度条是显示的*/
        private boolean enableProgress = true;
        private ViewGroup.LayoutParams mLayoutParams = null;
        private WebViewClient mWebViewClient;
        private WebChromeClient mWebChromeClient;
        private int mIndicatorColor = -1;
        private AgentWebSettings mAgentWebSettings;
        private WebCreator mWebCreator;
        private HttpHeaders mHttpHeaders = null;
        private IEventHandler mIEventHandler;
        private int height_dp = -1;
        private ArrayMap<String, Object> mJavaObject;
        private ChromeClientCallbackManager mChromeClientCallbackManager = new ChromeClientCallbackManager();
        private SecurityType mSecurityType = SecurityType.DEFAULT_CHECK;
        private WebView mWebView;
        private WebViewClientCallbackManager mWebViewClientCallbackManager = new WebViewClientCallbackManager();
        private boolean webClientHelper = true;
        private List<DownLoadResultListener> mDownLoadResultListeners = null;
        private IWebLayout mWebLayout = null;
        private PermissionInterceptor mPermissionInterceptor = null;
        private boolean isParallelDownload = false;
        private int icon = -1;
        private DownloadListener mDownloadListener = null;
        private AgentWebUIController mAgentWebUIController;
        private DefaultWebClient.OpenOtherPageWays openOtherPage = null;
        private boolean isInterceptUnkownScheme = false;
        private MiddleWareWebClientBase header;
        private MiddleWareWebClientBase tail;
        private MiddleWareWebChromeBase mChromeMiddleWareHeader = null;
        private MiddleWareWebChromeBase mChromeMiddleWareTail = null;
        private View errorView;
        private int errorLayout;
        private int reloadId;
        private int tag = -1;


        public AgentBuilder(@NonNull Activity activity, @NonNull Fragment fragment) {
            mActivity = activity;
            mFragment = fragment;
            tag = AgentWeb.FRAGMENT_TAG;
        }

        public AgentBuilder(@NonNull Activity activity) {
            mActivity = activity;
            tag = AgentWeb.ACTIVITY_TAG;
        }


        public IndicatorBuilderForFragment setAgentWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            return new IndicatorBuilderForFragment(this);
        }

        private PreAgentWeb buildAgentWeb() {
            if (tag == AgentWeb.FRAGMENT_TAG && this.mViewGroup == null)
                throw new NullPointerException("ViewGroup is null,please check you params");
            return new PreAgentWeb(HookManager.hookAgentWeb(new AgentWeb(this), this));
        }

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null)
                mJavaObject = new ArrayMap<>();
            mJavaObject.put(key, o);
        }

        private void addHeader(String k, String v) {

            if (mHttpHeaders == null)
                mHttpHeaders = HttpHeaders.create();
            mHttpHeaders.additionalHttpHeader(k, v);

        }
    }

    public static class IndicatorBuilderForFragment {
        AgentBuilder mAgentBuilder = null;

        public IndicatorBuilderForFragment(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilderForFragment useDefaultIndicator(int color) {
            this.mAgentBuilder.enableProgress = true;
            this.mAgentBuilder.mIndicatorColor = color;
            return new CommonBuilderForFragment(mAgentBuilder);
        }

        public CommonBuilderForFragment useDefaultIndicator() {
            this.mAgentBuilder.enableProgress = true;
            return new CommonBuilderForFragment(mAgentBuilder);
        }

        public CommonBuilderForFragment closeDefaultIndicator() {
            this.mAgentBuilder.enableProgress = false;
            this.mAgentBuilder.mIndicatorColor = -1;
            this.mAgentBuilder.height_dp = -1;
            return new CommonBuilderForFragment(mAgentBuilder);
        }

        public CommonBuilderForFragment setCustomIndicator(@NonNull BaseIndicatorView v) {
            if (v != null) {
                this.mAgentBuilder.enableProgress = true;
                this.mAgentBuilder.v = v;
                this.mAgentBuilder.isNeedDefaultProgress = false;
            } else {
                this.mAgentBuilder.enableProgress = true;
                this.mAgentBuilder.isNeedDefaultProgress = true;
            }

            return new CommonBuilderForFragment(mAgentBuilder);
        }

        public CommonBuilderForFragment setIndicatorColorWithHeight(@ColorInt int color, int height_dp) {
            this.mAgentBuilder.mIndicatorColor = color;
            this.mAgentBuilder.height_dp = height_dp;
            return new CommonBuilderForFragment(this.mAgentBuilder);
        }

    }


    public static class CommonBuilderForFragment {
        private AgentBuilder mAgentBuilder;

        public CommonBuilderForFragment(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilderForFragment setEventHanadler(@Nullable IEventHandler iEventHandler) {
            mAgentBuilder.mIEventHandler = iEventHandler;
            return this;
        }

        public CommonBuilderForFragment closeWebViewClientHelper() {
            mAgentBuilder.webClientHelper = false;
            return this;
        }


        public CommonBuilderForFragment setWebChromeClient(@Nullable WebChromeClient webChromeClient) {
            this.mAgentBuilder.mWebChromeClient = webChromeClient;
            return this;

        }

        public CommonBuilderForFragment setWebViewClient(@Nullable WebViewClient webChromeClient) {
            this.mAgentBuilder.mWebViewClient = webChromeClient;
            return this;
        }

        public CommonBuilderForFragment useMiddleWareWebClient(@NonNull MiddleWareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            if (this.mAgentBuilder.header == null) {
                this.mAgentBuilder.header = this.mAgentBuilder.tail = middleWrareWebClientBase;
            } else {
                this.mAgentBuilder.tail.enq(middleWrareWebClientBase);
                this.mAgentBuilder.tail = middleWrareWebClientBase;
            }
            return this;
        }

        public CommonBuilderForFragment useMiddleWareWebChrome(@NonNull MiddleWareWebChromeBase middleWareWebChromeBase) {
            if (middleWareWebChromeBase == null) {
                return this;
            }
            if (this.mAgentBuilder.mChromeMiddleWareHeader == null) {
                this.mAgentBuilder.mChromeMiddleWareHeader = this.mAgentBuilder.mChromeMiddleWareTail = middleWareWebChromeBase;
            } else {
                this.mAgentBuilder.mChromeMiddleWareTail.enq(middleWareWebChromeBase);
                this.mAgentBuilder.mChromeMiddleWareTail = middleWareWebChromeBase;
            }
            return this;
        }

        public CommonBuilderForFragment setMainFrameErrorView(@NonNull View view) {
            this.mAgentBuilder.errorView = view;
            return this;
        }

        public CommonBuilderForFragment setMainFrameErrorView(@LayoutRes int errorLayout, @IdRes int reloadId) {
            this.mAgentBuilder.errorLayout = errorLayout;
            this.mAgentBuilder.reloadId = reloadId;
            return this;
        }

        public CommonBuilderForFragment setAgentWebWebSettings(@Nullable AgentWebSettings agentWebSettings) {
            this.mAgentBuilder.mAgentWebSettings = agentWebSettings;
            return this;
        }

        public PreAgentWeb createAgentWeb() {
            return this.mAgentBuilder.buildAgentWeb();
        }

        public CommonBuilderForFragment setReceivedTitleCallback(@Nullable ChromeClientCallbackManager.ReceivedTitleCallback receivedTitleCallback) {
            this.mAgentBuilder.mChromeClientCallbackManager.setReceivedTitleCallback(receivedTitleCallback);
            return this;
        }

        public CommonBuilderForFragment addJavascriptInterface(@NonNull String name, @NonNull Object o) {
            this.mAgentBuilder.addJavaObject(name, o);
            return this;
        }

        public CommonBuilderForFragment setSecurityType(@NonNull SecurityType type) {
            this.mAgentBuilder.mSecurityType = type;
            return this;
        }

        public CommonBuilderForFragment openParallelDownload() {
            this.mAgentBuilder.isParallelDownload = true;
            return this;
        }

        public CommonBuilderForFragment setNotifyIcon(@DrawableRes int icon) {
            this.mAgentBuilder.icon = icon;
            return this;
        }

        public CommonBuilderForFragment setWebView(@Nullable WebView webView) {
            this.mAgentBuilder.mWebView = webView;
            return this;
        }

        public CommonBuilderForFragment setWebLayout(@Nullable IWebLayout iWebLayout) {
            this.mAgentBuilder.mWebLayout = iWebLayout;
            return this;
        }

        public CommonBuilderForFragment additionalHttpHeader(String k, String v) {
            this.mAgentBuilder.addHeader(k, v);

            return this;
        }

        public CommonBuilderForFragment setPermissionInterceptor(@Nullable PermissionInterceptor permissionInterceptor) {
            this.mAgentBuilder.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public CommonBuilderForFragment addDownLoadResultListener(@Nullable DownLoadResultListener downLoadResultListener) {

            if (this.mAgentBuilder.mDownLoadResultListeners == null) {
                this.mAgentBuilder.mDownLoadResultListeners = new ArrayList<>();
            }
            this.mAgentBuilder.mDownLoadResultListeners.add(downLoadResultListener);
            return this;
        }

        public CommonBuilderForFragment setAgentWebUIController(@Nullable AgentWebUIControllerImplBase agentWebUIController) {
            this.mAgentBuilder.mAgentWebUIController = agentWebUIController;
            return this;
        }

        public CommonBuilderForFragment setOpenOtherPageWays(@Nullable DefaultWebClient.OpenOtherPageWays openOtherPageWays) {
            this.mAgentBuilder.openOtherPage = openOtherPageWays;
            return this;
        }

        public CommonBuilderForFragment interceptUnkownScheme() {
            this.mAgentBuilder.isInterceptUnkownScheme = true;
            return this;
        }

    }

    private static final class PermissionInterceptorWrapper implements PermissionInterceptor {

        private WeakReference<PermissionInterceptor> mWeakReference;

        private PermissionInterceptorWrapper(PermissionInterceptor permissionInterceptor) {
            this.mWeakReference = new WeakReference<PermissionInterceptor>(permissionInterceptor);
        }

        @Override
        public boolean intercept(String url, String[] permissions, String a) {
            if (this.mWeakReference.get() == null) {
                return false;
            }
            return mWeakReference.get().intercept(url, permissions, a);
        }
    }


}
