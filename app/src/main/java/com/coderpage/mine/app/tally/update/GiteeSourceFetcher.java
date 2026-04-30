package com.coderpage.mine.app.tally.update;

import androidx.annotation.Keep;

import com.alibaba.fastjson.annotation.JSONField;
import com.coderpage.base.utils.LogUtils;
import com.coderpage.lib.update.ApkModel;
import com.coderpage.lib.update.Error;
import com.coderpage.lib.update.Result;
import com.coderpage.lib.update.SourceFetcher;
import com.coderpage.mine.BuildConfig;
import com.coderpage.mine.app.tally.common.error.ErrorCode;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import static com.coderpage.base.utils.LogUtils.makeLogTag;

/**
 * @author lingma
 * @since 0.6.0
 *
 * 用于从 Gitee 获取最新版本 APK 信息类；传递给 {@link com.coderpage.lib.update.Updater}；
 */
public class GiteeSourceFetcher implements SourceFetcher {
    private static final String TAG = makeLogTag(GiteeSourceFetcher.class);

    private static final String GITEE_API_BASE_URL = "https://gitee.com/api/v5/";

    /**
     * Gitee仓库所有者名称
     * 对于私有仓库，需要设置正确的所有者名称
     */
    private static final String GITEE_OWNER = "rocks-by-the-lake";

    /**
     * Gitee仓库名称
     * 对于私有仓库，需要设置正确的仓库名称
     */
    private static final String GITEE_REPO = "mine";

    /**
     * Gitee访问令牌（用于访问私有仓库）
     * 如果是私有仓库，需要设置有效的访问令牌
     * 如果是公开仓库，可以留空
     */
    private static final String GITEE_ACCESS_TOKEN = "";

    /**
     * 用户名（备用认证方式）
     */
    private static final String GITEE_USERNAME = "";

    /**
     * 密码（备用认证方式）
     */
    private static final String GITEE_PASSWORD = "";

    @Override
    public Result<ApkModel, Error> fetchApkModel() {
        Result<ApkModel, Error> result = new Result<>();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // 添加访问令牌拦截器（如果提供了访问令牌）
        if (GITEE_ACCESS_TOKEN != null && !GITEE_ACCESS_TOKEN.isEmpty()) {
            clientBuilder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Authorization", "token " + GITEE_ACCESS_TOKEN);
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            });
        }

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((message) -> {
                LogUtils.LOGI(TAG, message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        OkHttpClient okHttpClient = clientBuilder.build();

        Retrofit apiRetrofit = new Retrofit.Builder()
                .baseUrl(GITEE_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(FastJsonConverterFactory.create())
                .build();
        GiteeApi api = apiRetrofit.create(GiteeApi.class);

        // 获取最新版本信息
        retrofit2.Response<GiteeRelease> response = null;
        try {
            response = api.fetchLatestRelease(GITEE_OWNER, GITEE_REPO).execute();
            if (!response.isSuccessful()) {
                result.setErr(new Error(response.code(), response.message()));
                return result;
            }

            GiteeRelease body = response.body();
            if (body == null) {
                result.setErr(new Error(ErrorCode.UNKNOWN, "获取版本信息失败"));
                return result;
            }

            GiteeReleaseAsset targetAsset = null;
            List<GiteeReleaseAsset> assets = body.getAssets();
            if (assets != null) {
                for (GiteeReleaseAsset asset : assets) {
                    if (asset.getName() != null && asset.getName().endsWith(".apk")) {
                        targetAsset = asset;
                        break;
                    }
                }
            }

            if (targetAsset == null) {
                result.setErr(new Error(ErrorCode.UNKNOWN, "未找到APK文件"));
                return result;
            }

            GiteeApkModel apkModel = new GiteeApkModel();
            apkModel.setAppName("Mine");
            apkModel.setVersionName(body.getName());
            // 尝试从标签名中提取版本号
            String tag = body.getTag_name();
            apkModel.setVersionCode(parseVersionCode(tag));
            apkModel.setChangeLog(body.getBody());
            apkModel.setDownloadUrl(targetAsset.getBrowser_download_url());
            apkModel.setFileSize(targetAsset.getSize());

            result.setData(apkModel);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.setErr(new Error(ErrorCode.UNKNOWN, e.getMessage()));
            return result;
        } finally {
            // Response body is consumed; no explicit close needed
        }
    }

    private int parseVersionCode(String tagName) {
        if (tagName == null) return 0;
        try {
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            String[] parts = version.split("\\.");
            int code = 0;
            for (int i = 0; i < Math.min(parts.length, 3); i++) {
                code = code * 100 + Integer.parseInt(parts[i]);
            }
            return code;
        } catch (Exception e) {
            return 0;
        }
    }

    interface GiteeApi {
        @GET("repos/{owner}/{repo}/releases/latest")
        Call<GiteeRelease> fetchLatestRelease(@Path("owner") String owner, @Path("repo") String repo);
    }

    @Keep
    public static class GiteeRelease {
        @JSONField(name = "id")
        private int id;

        @JSONField(name = "tag_name")
        private String tag_name;

        @JSONField(name = "name")
        private String name;

        @JSONField(name = "body")
        private String body;

        @JSONField(name = "assets")
        private List<GiteeReleaseAsset> assets;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTag_name() {
            return tag_name;
        }

        public void setTag_name(String tag_name) {
            this.tag_name = tag_name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public List<GiteeReleaseAsset> getAssets() {
            return assets;
        }

        public void setAssets(List<GiteeReleaseAsset> assets) {
            this.assets = assets;
        }
    }

    @Keep
    public static class GiteeReleaseAsset {
        @JSONField(name = "name")
        private String name;

        @JSONField(name = "size")
        private long size;

        @JSONField(name = "browser_download_url")
        private String browser_download_url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getBrowser_download_url() {
            return browser_download_url;
        }

        public void setBrowser_download_url(String browser_download_url) {
            this.browser_download_url = browser_download_url;
        }
    }

    public static class GiteeApkModel implements ApkModel {
        private String appName;
        private String versionName;
        private int versionCode;
        private String changeLog;
        private String downloadUrl;
        private long fileSize;

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        @Override
        public long getBuildCode() {
            return versionCode;
        }

        @Override
        public String getVersion() {
            return versionName;
        }

        @Override
        public String getName() {
            return appName;
        }

        @Override
        public String getChangelog() {
            return changeLog;
        }

        @Override
        public String getDownloadUrl() {
            return downloadUrl;
        }

        @Override
        public long getApkSizeBytes() {
            return fileSize;
        }
    }
}
