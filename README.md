# Mine
- 自己写的一个记账本应用, 简单干净.
- 应用中使用的图标来自 [iconfont](http://iconfont.cn/) 和 [material-design-icons](https://github.com/google/material-design-icons)
- 原作者微信公众号 搜索:MINE应用

## 碎碎念
- 我其实非常喜欢这个项目,用了大概有2年时间. 期间没有遇到问题, 但是觉得打开输入金额, 标签, 真的太麻烦了. 我想要让他简单记录,
自动记录. 是的现在你可以使用分享功能到记账本, 这样你就可以记录了. 非常的方便, 无论是截图分享, 还是相册分享都可以哦. 另外还实
现了短信自动记录, 这是我梦寐以求的功能. 如果你担心隐私问题可以选择不使用短信功能.
- 我不是专业的安卓开发者, 代码已经尽可能优化了. 所以如果你是专业的安卓开发者, 使用后同样喜欢该项目, 可以联系我加入到项目.
- 原作者新版的v0.8.0很漂亮的, 可惜没有开源, 也没有我想要的功能. 叹! 我下载一个放发行版里面了, 需要的自己拿.

## 新功能
- ai能力来源于阿里云, 需要申请阿里百炼的账户: [阿里百炼控制台](https://bailian.console.aliyun.com/?tab=globalset#/efm/api_key)
- ai调用的key是必填的, 否则无法使用. 模型名称可以不填, 我默认给了一个(qwen2.5-vl-32b-instruct), 0.7.3(正式版)费用的话嗯,,,,,目
前输入价格：8元/百万tokens 输出价格：24元/百万tokens, 1080x2400的图片加上提示词加上输出大概是3分/次. 0.7.4版本
中我增加了图片压缩, (qwen2.5-vl-32b-instruct)大概0.5分/每次
- 短信能力基于安卓, 但是需要一些敏感权限, 短信识别是依靠关键词, 如果短信中同时存在支付类和收入类关键词, 那么就不会记录.
例如: 支付宝收款: 20元. 存在支付和收款关键词, 那么就会忽略. 当然"支付宝"这种关键字做了处理, 识别的了.
- 在原版的0.6.2版本的基础上增加了些新功能:

- - 增加ai调用识别能力
- - 增加了短信自动识别能力 
- - 增加清空全部记录
- - 增加自动备份功能
- - 增加文件导出功能
- - 增加账单记录筛选功能

## 链接, 项目演示
- 我的博客简单介绍了项目  [阿旭的时光瓶](http://myblog.love/articles/101)
- 原作者代码已托管在 GitHub(https://github.com/coderpage/Mine)
- 我的代码在Gitee(https://gitee.com/rocks-by-the-lake/mine)

## 下载
- 也可以直接点击下载链接
- 我的版本: https://pan.baidu.com/s/1CG6P8w69ynVRWIhjZAsxYw?pwd=wqbh 提取码: wqbh
- 作者原版：https://mp.weixin.qq.com/s?__biz=MzIzNTgwNzk1Nw==&mid=2247483666&idx=1&sn=a13dfe72241b69240bc7b502f7233457&chksm=e8e034a8df97bdbe5d270e8e7001f2a5bd1ddcbb14c75544024c8ce72ad8cd3cc82a997df7df&xtrack=1&scene=0&subscene=274&sessionid=1755250754&clicktime=1755258252&enterid=1755258252&ascene=7&fasttmpl_type=0&fasttmpl_fullversion=7865762-zh_CN-zip&fasttmpl_flag=0&realreporttime=1755258252347&devicetype=android-35&version=28003d5a&nettype=WIFI&abtest_cookie=AAACAA%3D%3D&lang=zh_CN&session_us=gh_a027cefd7741&countrycode=CN&exportkey=n_ChQIAhIQcC6QJ0pprWWOmdJ9mpMrExLxAQIE97dBBAEAAAAAAJVGMIRnCOsAAAAOpnltbLcz9gKNyK89dVj0EnjhlsLKEWNLpAn8jl5jjyQfrlHkFV0%2FVJDOY3LoHKknxq56x3W68Gd9mfnwPxFeMS1ZoJVZQNcwxzQRl1vEg9vsQbB8BM3UQAADeR9aR8P0T6OS928T5tZ%2BHkFC86medGh%2By07zMk8GY6itRmAXXBJUioezrmoG4wDKaJ9hCe7itmB8FSH%2BnosSrkWdaZnfyKl0vd2CWHzR%2Bvcw4Wj1LyrCpv%2B5waTHiR%2FkDH%2BxOAszcc1jfSscC%2BoO9X3TwJ9l1eGswbel5FFafeo%3D&pass_ticket=W5oyb1AOUMIGxEXhZFQASaP0KvV2k2TjDSWDiamG6cY%2FNf%2FIOrEdyi4EnceMVpQp&wx_header=3