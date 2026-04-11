package com.coderpage.mine.app.tally.module.setting;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.coderpage.mine.app.tally.common.router.TallyRouter;
import com.coderpage.mine.databinding.ActivityNotionDatabaseHelpBinding;
import com.coderpage.mine.ui.BaseActivity;

/**
 * Notion 数据库创建向导
 * 
 * @author Flandre Scarlet
 */
@Route(path = TallyRouter.NOTION_DATABASE_HELP)
public class NotionDatabaseHelpActivity extends BaseActivity {

    private ActivityNotionDatabaseHelpBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityNotionDatabaseHelpBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setupToolbar();
        loadHelpContent();
    }

    private void setupToolbar() {
        setToolbarAsBack(view -> finish());
        if (getToolbar() != null) {
            getToolbar().setTitle("创建 Notion 数据库");
        }
    }

    private void loadHelpContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<style>body{font-family:sans-serif;padding:16px;line-height:1.6}");
        sb.append("h1{color:#1a1a1a}h2{color:#333;margin-top:20px}");
        sb.append(".step{background:#f5f5f5;padding:12px;margin:12px 0}");
        sb.append(".warn{background:#fff3cd;padding:12px}");
        sb.append("table{width:100%;border-collapse:collapse}");
        sb.append("th,td{border:1px solid #ddd;padding:8px}");
        sb.append("th{background:#f0f0f0}");
        sb.append("code{background:#e8e8e8;padding:2px 6px}");
        sb.append(".req{color:#dc3545}");
        sb.append("</style></head><body>");
        sb.append("<h1>Notion 数据库创建指南</h1>");
        sb.append("<div class=\"warn\"><b>提示：</b>请使用电脑端 Notion 创建数据库</div>");
        sb.append("<h2>第一步：创建数据库</h2>");
        sb.append("<div class=\"step\">1. 打开 Notion 并登录<br>2. 创建新页面<br>3. 输入 /table 召唤块菜单<br>4. 选择 Table 创建数据库</div>");
        sb.append("<h2>第二步：配置字段</h2>");
        sb.append("<table><tr><th>字段名称</th><th>类型</th><th>必填</th><th>说明</th></tr>");
        sb.append("<tr><td>金额</td><td>Number</td><td class=\"req\">是</td><td>记账金额</td></tr>");
        sb.append("<tr><td>类型</td><td>Select</td><td class=\"req\">是</td><td>支出/收入</td></tr>");
        sb.append("<tr><td>分类</td><td>Text</td><td class=\"req\">是</td><td>分类名称</td></tr>");
        sb.append("<tr><td>时间</td><td>Date</td><td class=\"req\">是</td><td>记账日期</td></tr>");
        sb.append("<tr><td>备注</td><td>Text</td><td>否</td><td>备注说明</td></tr>");
        sb.append("</table>");
        sb.append("<h2>第三步：获取 Database ID</h2>");
        sb.append("<div class=\"step\">1. 点击数据库右上角 ... 菜单<br>2. 选择 Copy link to view<br>3. 链接中 32 位字符就是 Database ID</div>");
        sb.append("<h2>第四步：创建 Integration</h2>");
        sb.append("<div class=\"step\">1. 访问 notion.so/my-integrations<br>2. 点击 New integration<br>3. 填写名称并提交<br>4. 复制以 secret_ 开头的 Token</div>");
        sb.append("<h2>第五步：授权数据库</h2>");
        sb.append("<div class=\"step\">1. 打开数据库<br>2. 点击 ... 菜单<br>3. 选择 Connections<br>4. 添加您的 Integration</div>");
        sb.append("<h2>检查清单</h2>");
        sb.append("<div class=\"step\"><ul>");
        sb.append("<li>数据库已包含所有必填字段</li>");
        sb.append("<li>已复制 Database ID</li>");
        sb.append("<li>已复制 Integration Token</li>");
        sb.append("<li>已授权 Integration</li>");
        sb.append("</ul></div>");
        sb.append("<h2>提示</h2>");
        sb.append("<p>如果同步失败，请检查：</p><ul>");
        sb.append("<li>Token 是否正确（包含 secret_ 前缀）</li>");
        sb.append("<li>Database ID 是否为 32 位</li>");
        sb.append("<li>Integration 是否已添加到数据库</li>");
        sb.append("<li>网络连接是否正常</li>");
        sb.append("</ul></body></html>");
        
        mBinding.webView.getSettings().setJavaScriptEnabled(false);
        mBinding.webView.getSettings().setDefaultTextEncodingName("UTF-8");
        mBinding.webView.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-8", null);
    }
}
