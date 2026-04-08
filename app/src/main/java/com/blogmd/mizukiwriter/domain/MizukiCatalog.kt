package com.blogmd.mizukiwriter.domain

data class MizukiConfigExport(
    val title: String,
    val bindingName: String,
    val description: String,
)

data class MizukiFeatureDocument(
    val title: String,
    val path: String,
    val bindingName: String? = null,
    val description: String,
)

object MizukiCatalog {
    val configExports = listOf(
        MizukiConfigExport("站点主配置", "siteConfig", "全站基础信息、功能页开关、主题和布局配置。"),
        MizukiConfigExport("全屏壁纸", "fullscreenWallpaperConfig", "全屏壁纸轮播和透明度。"),
        MizukiConfigExport("导航栏", "navBarConfig", "导航链接和分组。"),
        MizukiConfigExport("个人资料", "profileConfig", "头像、昵称、简介和外链。"),
        MizukiConfigExport("许可协议", "licenseConfig", "默认版权协议。"),
        MizukiConfigExport("固定链接", "permalinkConfig", "文章 permalink 规则。"),
        MizukiConfigExport("代码块", "expressiveCodeConfig", "代码主题与切换行为。"),
        MizukiConfigExport("评论系统", "commentConfig", "Twikoo / Giscus 评论配置。"),
        MizukiConfigExport("分享配置", "shareConfig", "文章分享开关。"),
        MizukiConfigExport("公告", "announcementConfig", "侧栏公告内容与链接。"),
        MizukiConfigExport("音乐播放器", "musicPlayerConfig", "播放器模式和歌单参数。"),
        MizukiConfigExport("页脚", "footerConfig", "自定义 HTML 页脚。"),
        MizukiConfigExport("侧边栏布局", "sidebarLayoutConfig", "侧栏组件布局和动画。"),
        MizukiConfigExport("樱花特效", "sakuraConfig", "樱花数量、透明度与速度。"),
        MizukiConfigExport("看板娘", "pioConfig", "Pio 模型开关与文案。"),
        MizukiConfigExport("相关文章", "relatedPostsConfig", "相关文章推荐条数。"),
        MizukiConfigExport("随机文章", "randomPostsConfig", "随机文章推荐条数。"),
    )

    val featureDocuments = listOf(
        MizukiFeatureDocument("关于页正文", "src/content/spec/about.md", description = "关于页 Markdown 正文。"),
        MizukiFeatureDocument("友链申请说明", "src/content/spec/friends.md", description = "友链页补充说明 Markdown。"),
        MizukiFeatureDocument("友链数据", "src/data/friends.ts", bindingName = "friendsData", description = "友链卡片列表。"),
        MizukiFeatureDocument("日记条目", "src/data/diary.ts", bindingName = "diaryData", description = "日记页面记录列表。"),
        MizukiFeatureDocument("项目数据", "src/data/projects.ts", bindingName = "projectsData", description = "项目页面卡片列表。"),
        MizukiFeatureDocument("技能数据", "src/data/skills.ts", bindingName = "skillsData", description = "技能页面卡片列表。"),
        MizukiFeatureDocument("时间线数据", "src/data/timeline.ts", bindingName = "timelineData", description = "时间线页面事件列表。"),
        MizukiFeatureDocument("设备数据", "src/data/devices.ts", bindingName = "devicesData", description = "设备页面分组数据。"),
        MizukiFeatureDocument("番剧数据", "src/data/anime.ts", bindingName = "localAnimeList", description = "本地番剧数据模式。"),
    )

    const val diaryPath = "src/data/diary.ts"
    const val diaryBinding = "diaryData"
}
