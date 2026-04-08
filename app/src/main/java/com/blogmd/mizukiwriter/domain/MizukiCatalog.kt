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

data class FieldPresentation(
    val label: String,
    val help: String? = null,
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

    private val commonFieldLabels = mapOf(
        "title" to FieldPresentation("标题"),
        "description" to FieldPresentation("简介"),
        "name" to FieldPresentation("名称"),
        "content" to FieldPresentation("内容"),
        "summary" to FieldPresentation("摘要"),
        "subtitle" to FieldPresentation("副标题"),
        "url" to FieldPresentation("链接"),
        "href" to FieldPresentation("跳转链接"),
        "link" to FieldPresentation("链接"),
        "links" to FieldPresentation("链接列表"),
        "icon" to FieldPresentation("图标"),
        "image" to FieldPresentation("图片"),
        "avatar" to FieldPresentation("头像"),
        "cover" to FieldPresentation("封面"),
        "banner" to FieldPresentation("横幅"),
        "tags" to FieldPresentation("标签"),
        "category" to FieldPresentation("分类"),
        "date" to FieldPresentation("日期"),
        "createdAt" to FieldPresentation("创建时间"),
        "updatedAt" to FieldPresentation("更新时间"),
        "startDate" to FieldPresentation("开始时间"),
        "endDate" to FieldPresentation("结束时间"),
        "status" to FieldPresentation("状态"),
        "platform" to FieldPresentation("平台"),
        "episodes" to FieldPresentation("集数"),
        "year" to FieldPresentation("年份"),
        "author" to FieldPresentation("作者"),
        "role" to FieldPresentation("角色"),
        "skills" to FieldPresentation("技能"),
        "items" to FieldPresentation("条目列表"),
        "label" to FieldPresentation("标签名"),
        "value" to FieldPresentation("值"),
        "enabled" to FieldPresentation("启用"),
        "enable" to FieldPresentation("启用"),
        "disabled" to FieldPresentation("禁用"),
        "profile" to FieldPresentation("个人资料"),
        "friends" to FieldPresentation("友链"),
        "projects" to FieldPresentation("项目"),
        "timeline" to FieldPresentation("时间线"),
        "devices" to FieldPresentation("设备"),
        "anime" to FieldPresentation("番剧"),
        "diary" to FieldPresentation("日记"),
        "comment" to FieldPresentation("评论"),
        "footer" to FieldPresentation("页脚"),
        "theme" to FieldPresentation("主题"),
        "lang" to FieldPresentation("语言"),
        "timezone" to FieldPresentation("时区"),
        "repo" to FieldPresentation("仓库"),
        "owner" to FieldPresentation("所有者"),
        "license" to FieldPresentation("许可"),
        "licenseName" to FieldPresentation("许可名称"),
        "source" to FieldPresentation("来源"),
        "homepage" to FieldPresentation("主页"),
        "id" to FieldPresentation("编号"),
    )

    fun resolveFieldLabel(path: String, bindingName: String?, key: String): String {
        val documentSpecific = when {
            path == "src/config.ts" && bindingName == "siteConfig" -> siteConfigLabels[key]
            path == "src/data/friends.ts" -> friendsLabels[key]
            path == "src/data/diary.ts" -> diaryLabels[key]
            path == "src/data/projects.ts" -> projectLabels[key]
            path == "src/data/skills.ts" -> skillsLabels[key]
            path == "src/data/timeline.ts" -> timelineLabels[key]
            path == "src/data/devices.ts" -> deviceLabels[key]
            path == "src/data/anime.ts" -> animeLabels[key]
            path.endsWith(".md") -> markdownLabels[key]
            else -> null
        }
        return documentSpecific?.label ?: commonFieldLabels[key]?.label ?: key
    }

    fun rootSectionLabel(path: String, bindingName: String?): String = when {
        path.endsWith(".md") -> "页面信息"
        path == "src/config.ts" && bindingName != null ->
            configExports.firstOrNull { it.bindingName == bindingName }?.title ?: "配置内容"
        bindingName != null ->
            featureDocuments.firstOrNull { it.path == path && it.bindingName == bindingName }?.title ?: "结构化内容"
        else -> "远程内容"
    }

    private val markdownLabels = mapOf(
        "title" to FieldPresentation("标题"),
        "description" to FieldPresentation("简介"),
        "published" to FieldPresentation("发布"),
        "draft" to FieldPresentation("草稿"),
        "tags" to FieldPresentation("标签"),
        "category" to FieldPresentation("分类"),
        "date" to FieldPresentation("日期"),
    )

    private val siteConfigLabels = mapOf(
        "title" to FieldPresentation("站点标题"),
        "description" to FieldPresentation("站点简介"),
        "lang" to FieldPresentation("站点语言"),
        "timezone" to FieldPresentation("站点时区"),
        "theme" to FieldPresentation("主题"),
        "navBarConfig" to FieldPresentation("导航栏配置"),
        "profileConfig" to FieldPresentation("个人资料配置"),
        "featurePages" to FieldPresentation("功能页开关"),
    )

    private val friendsLabels = mapOf(
        "name" to FieldPresentation("友链名称"),
        "description" to FieldPresentation("友链简介"),
        "url" to FieldPresentation("友链地址"),
        "avatar" to FieldPresentation("头像"),
    )

    private val diaryLabels = mapOf(
        "content" to FieldPresentation("日记内容"),
        "date" to FieldPresentation("记录日期"),
        "tags" to FieldPresentation("标签"),
    )

    private val projectLabels = mapOf(
        "name" to FieldPresentation("项目名称"),
        "description" to FieldPresentation("项目简介"),
        "url" to FieldPresentation("项目链接"),
        "tags" to FieldPresentation("技术标签"),
    )

    private val skillsLabels = mapOf(
        "name" to FieldPresentation("技能名称"),
        "description" to FieldPresentation("技能说明"),
        "icon" to FieldPresentation("技能图标"),
    )

    private val timelineLabels = mapOf(
        "title" to FieldPresentation("事件标题"),
        "description" to FieldPresentation("事件描述"),
        "date" to FieldPresentation("事件日期"),
    )

    private val deviceLabels = mapOf(
        "name" to FieldPresentation("设备名称"),
        "description" to FieldPresentation("设备说明"),
        "items" to FieldPresentation("设备条目"),
    )

    private val animeLabels = mapOf(
        "title" to FieldPresentation("番剧标题"),
        "description" to FieldPresentation("番剧简介"),
        "episodes" to FieldPresentation("集数"),
        "tags" to FieldPresentation("标签"),
        "year" to FieldPresentation("年份"),
    )
}
