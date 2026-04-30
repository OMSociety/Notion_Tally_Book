package com.coderpage.mine.app.tally.update;

import androidx.annotation.Keep;

import com.alibaba.fastjson.annotation.JSONField;
import com.coderpage.lib.update.ApkModel;
import com.coderpage.lib.update.Error;
import com.coderpage.lib.update.Result;
import com.coderpage.lib.update.SourceFetcher;
import com.coderpage.mine.BuildConfig;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import static com.coderpage.base.utils.LogUtils.makeLogTag;

/**
 * GitHub 版本检查器
 * 
 * @author Flandre Scarlet
 */
public class GitHubSourceFetcher implements SourceFetcher {
    private static final String TAG = makeLogTag(GitHubSourceFetcher.class);

    private static final String GITHUB_API_BASE_URL = "https://api.github.com/";

    // GitHub 仓库信息
    private static final String GITHUB_OWNER = "OMSociety";
    private static final String GITHUB_REPO = "Notion_Tally_Book";

    @Override
    public Result<ApkModel, Error> fetchApkModel() {
        Result<ApkModel, Error> result = new Result<>();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((message) -> {
                com.coderpage.base.utils.LogUtils.LOGI(TAG, message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        OkHttpClient okHttpClient = clientBuilder.build();

        Retrofit apiRetrofit = new Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(FastJsonConverterFactory.create())
                .build();
        
        GitHubApi api = apiRetrofit.create(GitHubApi.class);

        Response<GitHubRelease> response = null;
        try {
            response = api.fetchLatestRelease(GITHUB_OWNER, GITHUB_REPO).execute();

            if (!response.isSuccessful()) {
                result.setErr(new Error(response.code(), response.message()));
                return result;
            }

            GitHubRelease body = response.body();
            if (body == null) {
                result.setErr(new Error(-1, "获取版本信息失败"));
                return result;
            }

            // 查找 APK 文件
            GitHubReleaseAsset targetAsset = null;
            List<GitHubReleaseAsset> assets = body.getAssets();
            if (assets != null) {
                for (GitHubReleaseAsset asset : assets) {
                    if (asset.getName() != null && asset.getName().endsWith(".apk")) {
                        targetAsset = asset;
                        break;
                    }
                }
            }

            if (targetAsset == null) {
                result.setErr(new Error(-1, "未找到APK文件"));
                return result;
            }

            GitHubApkModel apkModel = new GitHubApkModel();
            apkModel.setAppName("Notion记账本");
            apkModel.setVersionName(body.getTagName());
            apkModel.setVersionCode(parseVersionCode(body.getTagName()));
            apkModel.setChangeLog(body.getBody());
            apkModel.setDownloadUrl(targetAsset.getBrowserDownloadUrl());
            apkModel.setFileSize(targetAsset.getSize());

            result.setData(apkModel);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.setErr(new Error(-1, e.getMessage()));
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

    interface GitHubApi {
        @GET("repos/{owner}/{repo}/releases/latest")
        retrofit2.Call<GitHubRelease> fetchLatestRelease(@Path("owner") String owner, @Path("repo") String repo);
    }

    @Keep
    public static class GitHubRelease {
        @JSONField(name = "tag_name")
        private String tagName;

        @JSONField(name = "name")
        private String name;

        @JSONField(name = "body")
        private String body;

        @JSONField(name = "assets")
        private List<GitHubReleaseAsset> assets;

        public String getTagName() { return tagName; }
        public String getName() { return name; }
        public String getBody() { return body; }
        public List<GitHubReleaseAsset> getAssets() { return assets; }
    }

    @Keep
    public static class GitHubReleaseAsset {
        @JSONField(name = "name")
        private String name;

        @JSONField(name = "size")
        private long size;

        @JSONField(name = "browser_download_url")
        private String browserDownloadUrl;

        public String getName() { return name; }
        public long getSize() { return size; }
        public String getBrowserDownloadUrl() { return browserDownloadUrl; }
    }

    public static class GitHubApkModel implements ApkModel {
        private String appName;
        private String versionName;
        private int versionCode;
        private String changeLog;
        private String downloadUrl;
        private long fileSize;

        public void setAppName(String appName) { this.appName = appName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
        public void setVersionCode(int versionCode) { this.versionCode = versionCode; }
        public void setChangeLog(String changeLog) { this.changeLog = changeLog; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        @Override
        public long getBuildCode() { return versionCode; }
        @Override
        public String getVersion() { return versionName; }
        @Override
        public String getName() { return appName; }
        @Override
        public String getChangelog() { return changeLog; }
        @Override
        public String getDownloadUrl() { return downloadUrl; }
        @Override
        public long getApkSizeBytes() { return fileSize; }
    }
}
